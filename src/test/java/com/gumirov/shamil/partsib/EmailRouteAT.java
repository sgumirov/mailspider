package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Automation FTP endpoint test with local FTP daemon
 */
public class EmailRouteAT extends CamelTestSupport {
  private static final String pricehookId = "1.2.0.1";
  final int httpPort = 8888;
  private String httpendpoint="/endpoint";
  final String httpUrl = "http://127.0.0.1:"+ httpPort+httpendpoint;
  private int imapport = 3993;
  final String imapHost = "127.0.0.1"+":"+imapport;
  private List<String> filenames = Arrays.asList("sample1.csv", "Прайс лист1.csv");
  private byte[] contents = "a,b,c,d,e,1,2,3".getBytes();

  {
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
  }

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTPS_IMAPS);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(httpPort));

  final String login = "login-id", pwd = "password", to = "partsibprice@mail.ru";

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

//  MainRouteBuilder builder;

  @Before
  public void setup() throws Exception {
    //clear:
    new File(config.get("email.idempotent.repo")).delete();
    //http mock
    stubFor(post(urlEqualTo(httpendpoint))
        .willReturn(aResponse()
            .withStatus(200)));
    //disable ssl cert checking for imaps connections
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());

    context.setTracing(false);
    context.setMessageHistory(false);
    context.start();
  }

  public Runnable validate(String filename, int parts, String pricehookId) {
    return () -> {
      try {
        for (int i = 0; i < parts; ++i) {
          WireMock.verify(
              WireMock.postRequestedFor(urlPathEqualTo(httpendpoint))
                  .withHeader("X-Filename", equalTo(java.util.Base64.getEncoder().encodeToString(filename.getBytes("UTF-8"))))
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

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  @Test
  public void test() throws Exception{
    sendMessage();
    Thread.sleep(2000);
    validate(filenames.get(0), 1, pricehookId).run();
    validate(filenames.get(1), 1, pricehookId).run();
    verify(2, postRequestedFor(urlEqualTo(httpendpoint)));
  }

  private void sendMessage() throws MessagingException {
    GreenMailUser user = greenMail.setUser(to, login, pwd);
    HashMap<String, byte[]> attach = new HashMap<>();
    attach.put(filenames.get(0), contents);
    attach.put(filenames.get(1), contents);
    user.deliver(createMimeMessage(to, "shamil.gumirov@gmail.com","Прайс-лист компании ASVA", attach));
  }

  private MimeMessage createMimeMessage(String to, String from, String subject, Map<String, byte[]> attachments) throws MessagingException {
    MimeMessage msg = GreenMailUtil.createTextEmail(to, from, subject, "body", greenMail.getImaps().getServerSetup());
    Multipart multipart = new MimeMultipart();
    for (String fname : attachments.keySet()) {
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      byte[] file = attachments.get(fname);
      messageBodyPart.setDataHandler(new DataHandler(file, "application/octet-stream"));
      messageBodyPart.setFileName(fname);
      multipart.addBodyPart(messageBodyPart);
    }
    msg.setContent(multipart);
    return msg;
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
        email.url = imapHost;
        email.user = login;
        email.pwd = pwd;
        email.delay = "1000";
        e.email.add(email);
        return e;
      }

      @Override
      public ArrayList<EmailRule> getEmailAcceptRules() throws IOException {
        ArrayList<EmailRule> rules = new ArrayList<>();
        EmailRule r1 = new EmailRule();
        r1.header="From";
        r1.contains="shamil";
        rules.add(r1);
        return rules;
      }

      @Override
      public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
        PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
        r1.header = "Subject";
        r1.contains = "ASVA";
        r1.pricehookid = pricehookId;
        PricehookIdTaggingRule r2 = new PricehookIdTaggingRule();
        r2.header = "Subject";
        r2.contains = "АСВА";
        r2.pricehookid = pricehookId;
        return Arrays.asList(r1, r2);
      }
    };
  }
}
