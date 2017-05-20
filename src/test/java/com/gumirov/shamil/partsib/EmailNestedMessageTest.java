package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 *
 */
public class EmailNestedMessageTest extends CamelTestSupport {
  private static Logger log = LoggerFactory.getLogger(EmailNestedMessageTest.class.getSimpleName());
  //properties
  final String login = "login-id", pwd = "password", to = "partsibprice@mail.ru";
  private static final String pricehookId = "1.2.0.1";
  final int httpPort = 8888;
  public String httpendpoint="/endpoint";
  final String httpUrl = "http://127.0.0.1:"+ httpPort+httpendpoint;
  private int pop3port = 3110;
  final String pop3Url = "pop3://127.0.0.1"+":"+pop3port;
  private List<String> filenames = Arrays.asList("sample1.csv", "Прайс лист1.csv", "wrongfile.jpg");
  private byte[] contents = "a,b,c,d,e,1,2,3".getBytes();
  { //ssl init
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
  }

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(httpPort));

  ConfiguratorFactory cfactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
      kv.put("email.enabled", "true");
      kv.put("local.enabled", "0");
      kv.put("ftp.enabled",   "0");
      kv.put("http.enabled",  "0");
      kv.put("output.url", httpUrl);
      kv.put("endpoints.config.filename", "target/classes/test_local_endpoints.json");
      kv.put("email.accept.rules.config.filename=", "src/main/resources/email_accept_rules.json");
    }
  };
  Configurator config = cfactory.getConfigurator();

  @Override
  public void setUp() throws Exception {
    super.setUp(); //todo?
    context.setTracing(false);
    context.setMessageHistory(false);
  }

  @Before
  public void prepareHttpdOK() {
    stubFor(post(urlEqualTo(httpendpoint))
        .willReturn(aResponse()
            .withStatus(200)));
  }

  @Test
  public void test() throws Exception{
    WireMock.reset();
    prepareHttpdOK();
    execute(() -> {
          sendEml(getClass().getClassLoader().getResourceAsStream("fixed.eml"));
        },
        20000,
        //check filenames and tags
        validate("Остатки Москва 17.04.xlsx", 2, pricehookId),
        validate("Остатки новолайн 17.04.xls", 1, pricehookId),
        validate("LONG_SHITTY_NAME.xlsx", 1, pricehookId),
        () -> {
          //total 4 POSTs
          verify(4, postRequestedFor(urlEqualTo(httpendpoint)));
        },
        () -> {
          //TRANSACTION: deleted processed message
          Retriever retriever = new Retriever(greenMail.getPop3());
          Message[] messages = retriever.getMessages(login, pwd);
          assertEquals(0, messages.length);
        }
    );
  }

  @Test
  public void testBareAttachmentIssue() throws Exception{
    //use POP3: this does NOT work under IMAP (due to GreenMail bug)
    log.info("Test: testBareAttachmentIssue");
    WireMock.reset();
    prepareHttpdOK();
    final String expectedName = "Прайс-лист за 2017-04-17.xls";
    execute(
      () -> sendEml(getClass().getClassLoader().getResourceAsStream("issue.eml")),
      20000,
      () -> {
        //TRANSACTION: deleted processed message
        Retriever retriever = new Retriever(greenMail.getPop3());
        Message[] messages = retriever.getMessages(login, pwd);
        assertEquals(0, messages.length);
      },
      //check filenames and tags
      validate(expectedName, 3, pricehookId)
    );
  }

  @Test
  public void testNewIssue() throws Exception{
    log.info("Test: testBareAttachmentIssue");
    WireMock.reset();
    prepareHttpdOK();
    final String expectedName = "NSB-VTRPrice-O4.xls";
    execute(
      () -> {
        sendEml(getClass().getClassLoader().getResourceAsStream("20_05_issue.eml"));
        try{
          context.start();
        }catch(Exception e) {
          log.error("Cannot start Camel");
        }
      },
      20000,
      //check filenames and tags
      () -> assureMailConsumed(),
      validate(expectedName, 1, pricehookId)
    );
  }

  public void assureMailConsumed() {
    Retriever retriever = new Retriever(greenMail.getPop3());
    Message[] messages = retriever.getMessages(login, pwd);
    assertEquals(0, messages.length);
  }

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new MainRouteBuilder(config){
      @Override
      public List<Plugin> getPlugins() {
        return null;
      }

      @Override
      public Endpoints getEndpoints() throws IOException {
        Endpoints e = new Endpoints();
        e.ftp=new ArrayList<>();
        e.http=new ArrayList<>();
        e.email = new ArrayList<>();
        Endpoint email = new Endpoint();
        email.id = "Test-EMAIL-01";

        email.url = pop3Url;
        email.user = login;
        email.pwd = pwd;
        email.parameters = new HashMap<String, String>(){{ put("delete", "true" ); }};

        email.delay = "10000";
        e.email.add(email);
        return e;
      }

      @Override
      public List<String> getExtensionsAcceptList() {
        return Arrays.asList("xls","csv","txt","xlsx");
      }

      @Override
      public ArrayList<EmailAcceptRule> getEmailAcceptRules() throws IOException {
        ArrayList<EmailAcceptRule> rules = new ArrayList<>();
        EmailAcceptRule r1 = new EmailAcceptRule();
        r1.header="From";
        r1.contains="@";
        rules.add(r1);
        return rules;
      }

      @Override
      public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
        PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
        r1.header = "From";
        r1.contains = "@";
        r1.pricehookid = pricehookId;
        return Arrays.asList(r1);
      }
    };
  }

  void execute(Runnable test, long timeWait, Runnable ... validators) throws InterruptedException {
    test.run();
    Thread.sleep(timeWait);
    for (Runnable r : validators) {
      r.run();
    }
  }

  public void sendEml(InputStream emlIs) {
    System.setProperty("mail.debug", "true");
//    Session ses = GreenMailUtil.getSession(greenMail.getImap().getServerSetup());
    Session ses = GreenMailUtil.getSession(greenMail.getPop3().getServerSetup());
    ses.setDebug(true);
    ses.getProperties().setProperty("mail.imap.partialfetch", "false");
    try {
      MimeMessage msg = new MimeMessage(ses, emlIs);
      GreenMailUser user = greenMail.setUser(to, login, pwd);
      user.deliver(msg);
      log.info("sendEml(): EML was sent");
    } catch (MessagingException e) {
      throw new RuntimeException("Fuck", e);
    }
  }

  public Runnable validate(String filename, int parts, String pricehookId) {
    return () -> {
      try {
        for (int i = 0; i < parts; ++i) {
          WireMock.verify(
              WireMock.postRequestedFor(urlPathEqualTo(httpendpoint))
                  .withHeader("X-Filename", equalTo(Base64.getEncoder().encodeToString(filename.getBytes("UTF-8"))))
                  .withHeader("X-Pricehook", equalTo(pricehookId))
                  .withHeader("X-Part", equalTo(""+i))
                  .withHeader("X-Parts-Total", equalTo(""+parts))
          );
        }
      } catch (UnsupportedEncodingException e) {
        //e.printStackTrace();
      }
    };
  }
}
