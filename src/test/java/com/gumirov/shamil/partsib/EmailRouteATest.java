package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.NameChangerPlugin;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.UnseenRetriever;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.*;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Automation endpoint test with local daemon.
 * This is first generation test. <br/>TODO @shamilg rewrite it using {@link AbstractMailAutomationTest}
 * <p>Please note that this test prints lots of errors, that's OK!</p>
 */
public class EmailRouteATest extends CamelTestSupport {

  { //ssl init
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
  }

  private static Logger LOG = LoggerFactory.getLogger(EmailRouteATest.class.getSimpleName());
  private final String pricehookId = "1.2.0.1";
  private final int httpPort = 8888;
  private final String httpendpoint="/endpoint";
  private final String httpUrl = "http://127.0.0.1:"+ httpPort+httpendpoint;
  private final int imapport = 3143;
  private final String imapUrl = "imap://127.0.0.1"+":"+imapport;
  private final List<String> filenames = Arrays.asList("примерПрайса.txt", "Прайс лист1.csv", "wrongfile.jpg");
  private final byte[] contents = "a,b,c,d,e,1,2,3".getBytes();
  private final String login = "login-id", pwd = "password", to = "partsibprice@mail.ru";

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP);
  @Rule
  public final GreenMailRule notificationsSmtp = new GreenMailRule(new ServerSetup(3125, "127.0.0.1", "smtp"));
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

  @Before
  public void setup() throws Exception {
    //disable ssl cert checking for imaps connections
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
    //init mock servers
    notificationsSmtp.reset();
    notificationsSmtp.setUser(login, pwd);
    greenMail.reset();
    greenMail.setUser(login, pwd);
    //start context manually because setAdvice()==true
    context.setTracing(false);
    context.start();
  }

  public void prepareHttpdOK() {
    reset();
    stubFor(post(urlEqualTo(httpendpoint))
        .willReturn(aResponse()
            .withStatus(200)));
  }
  
  private void prepareHttpdFailFirstNTimes(int n) {
    reset();
    stubFor(post(urlEqualTo(httpendpoint)).inScenario("fail")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse()
            .withStatus(500).withStatusMessage("Internal server error"))
        .willSetStateTo("STATE_"+1));
    for (int i = 1; i < n; ++i) {
      stubFor(post(urlEqualTo(httpendpoint)).inScenario("fail")
          .whenScenarioStateIs("STATE_"+i)
          .willReturn(aResponse()
              .withStatus(500).withStatusMessage("Internal server error"))
          .willSetStateTo( i < n-1 ?("STATE_"+(i+1)) : "OK"));
    }
    stubFor(post(urlEqualTo(httpendpoint)).inScenario("fail")
        .whenScenarioStateIs("OK")
        .willReturn(aResponse()
            .withStatus(200)));
  }

  @Test
  public void test() throws Exception{
    prepareHttpdOK();
    execute(() -> sendMessage(filenames), 
        30000,
        validate(filenames.get(0)+".csv", 1, pricehookId),
        validate(filenames.get(1)+".csv", 1, pricehookId),
        () -> verify(2, postRequestedFor(urlEqualTo(httpendpoint))),
        () -> {
          UnseenRetriever unseenRetriever = new UnseenRetriever(greenMail.getImap());
          Message[] messages = unseenRetriever.getMessages(login, pwd);
          assertEquals(0, messages.length);
        },
        () -> assertTrue(notificationsSmtp.getReceivedMessages().length > 0)
    );
  }

  @Test
  public void testRealEmails() throws Exception {
    //does not work under POP3
    LOG.info("Test: testRealEmails");
    prepareHttpdOK();
    execute(() -> {
          sendEml(getClass().getClassLoader().getResourceAsStream("real-mail-1.eml"));
          sendEml(getClass().getClassLoader().getResourceAsStream("real-mail-2.eml"));
          sendEml(getClass().getClassLoader().getResourceAsStream("real-mail-3.eml"));
          sendEml(getClass().getClassLoader().getResourceAsStream("real-mail-4.eml"));
          sendEml(getClass().getClassLoader().getResourceAsStream("nocontent.eml"));
        },
        30000,
        //check filenames
//        validate(expectedName, 3, pricehookId),
        () -> {
          //TRANSACTION: deleted processed message
          UnseenRetriever retriever = new UnseenRetriever(greenMail.getImap());
          Message[] messages = retriever.getMessages(login, pwd);
          LOG.info("Messages not processed: "+messages.length);
          assertEquals(0, messages.length);
        },
        () -> assertTrue(notificationsSmtp.getReceivedMessages().length > 0)
    );
  }

  public void sendEml(InputStream emlIs) {
    System.setProperty("mail.debug", "true");
    Session ses = GreenMailUtil.getSession(greenMail.getImap().getServerSetup());
    ses.setDebug(true);
    ses.getProperties().setProperty("mail.imap.partialfetch", "false");
    try {
      MimeMessage msg = new MimeMessage(ses, emlIs);
      GreenMailUser user = greenMail.setUser(to, login, pwd);
      user.deliver(msg);
    } catch (MessagingException e) {
      throw new RuntimeException("Error while sending email", e);
    }
  }

  @Test
  public void testWithHttpFailures() throws Exception{
    prepareHttpdFailFirstNTimes(2);
    execute(() -> sendMessage(filenames),
        50000,
        validate(filenames.get(0)+".csv", 1, pricehookId),
        validate(filenames.get(1)+".csv", 1, pricehookId),
        () -> verify(4, postRequestedFor(urlEqualTo(httpendpoint))),
        () -> {
          //TRANSACTION: deleted processed message
          UnseenRetriever unseenRetriever = new UnseenRetriever(greenMail.getImap());
          Message[] messages = unseenRetriever.getMessages(login, pwd);
          assertEquals(0, messages.length);
        },
        () -> assertTrue(notificationsSmtp.getReceivedMessages().length > 0)
    );
  }

  void execute(Runnable test, long timeWait, Runnable ... validators) throws InterruptedException {
    test.run();
    Thread.sleep(timeWait);
    for (Runnable r : validators) {
      r.run();
    }
  }

  public Runnable validate(String filename, int parts, String pricehookId) {
    return () -> {
      try {
        for (int i = 0; i < parts; ++i) {
          verify(
              postRequestedFor(urlEqualTo(httpendpoint))
                  .withHeader("X-Filename", equalTo(Base64.getEncoder().encodeToString(filename.getBytes("UTF-8"))))
                  .withHeader("X-Pricehook", equalTo(pricehookId))
                  .withHeader("X-Part", equalTo(""+i))
                  .withHeader("X-Parts-Total", equalTo(""+parts))
          );
        }
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    };
  }

  private void sendMessage(List<String> filenames) {
    GreenMailUser user = greenMail.setUser(to, login, pwd);
    HashMap<String, byte[]> attach = new HashMap<>();
    try{
      for (String fn : filenames) {
        attach.put(MimeUtility.encodeText(fn), contents);
      }
      user.deliver(createMimeMessage(to, "someone@gmail.com","Прайс-лист компании ASVA Re", attach)); }
    catch (Exception e){throw new RuntimeException(e);}
  }

  private MimeMessage createMimeMessage(String to, String from, String subject, Map<String, byte[]> attachments) throws MessagingException {
    MimeMessage msg = GreenMailUtil.createTextEmail(to, from, subject, "body", greenMail.getImap().getServerSetup());
    Multipart multipart = new MimeMultipart();
    for (String fname : attachments.keySet()) {
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      byte[] file = attachments.get(fname);
      messageBodyPart.setDataHandler(new DataHandler(file, "application/vnd.octet-stream"));
      messageBodyPart.setFileName(fname);
      multipart.addBodyPart(messageBodyPart);
    }
    msg.setContent(multipart);
    return msg;
  }

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new MyMainRouteBuilder(config);
  }

  class MyMainRouteBuilder extends MainRouteBuilder{
    public MyMainRouteBuilder(Configurator config) {
      super(config);
    }

    @Override
    public List<Plugin> getPlugins() {
      return Arrays.asList(new NameChangerPlugin());
    }

    @Override
    public Endpoints getEndpoints() {
      Endpoints e = new Endpoints();
      e.ftp=new ArrayList<>();
      e.http=new ArrayList<>();
      e.email = new ArrayList<>();
      Endpoint email = new Endpoint();
      email.id = "EmailRouteTest-EMAIL-01";

      email.url = imapUrl;
      email.user = login;
      email.pwd = pwd;

      email.parameters = new HashMap<>();
      email.delay = "10000";

      e.email.add(email);
      return e;
    }

    @Override
    public ArrayList<EmailAcceptRule> getEmailAcceptRules() {
      ArrayList<EmailAcceptRule> rules = new ArrayList<>();
      EmailAcceptRule r1 = new EmailAcceptRule();
      r1.header="From";
      r1.contains="@";
      rules.add(r1);
      return rules;
    }

    @Override
    public Properties loadNotificationConfig(String fname) {
      return AbstractMailAutomationTest.createNotificationsConfig(
          "3000", "partsibprice@yahoo.com",
          login,
          "smtp://127.0.0.1:3125?username="+login+"&password="+pwd+"&debugMode=true"
      );
    }

    @Override
    public List<String> getExtensionsAcceptList() {
      return Arrays.asList("xls","csv","txt","xlsx");
    }

    @Override
    public List<PricehookIdTaggingRule> getPricehookConfig() {
      PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
      r1.header = "From";
      r1.contains = "@";
      r1.pricehookid = pricehookId;
      PricehookIdTaggingRule r2 = new PricehookIdTaggingRule();
      r2.header = "Subject";
      r2.contains = "АСВА";
      r2.pricehookid = pricehookId;
      PricehookIdTaggingRule r3 = new PricehookIdTaggingRule();
      r3.header = "Subject";
      r3.contains = "Прайс";
      r3.pricehookid = pricehookId;
      return Arrays.asList(r1, r2, r3);
    }
  }
}

