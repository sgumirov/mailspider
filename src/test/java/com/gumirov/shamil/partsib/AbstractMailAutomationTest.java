package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.*;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.AttachmentVerifier;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoaderProvider;
import com.gumirov.shamil.partsib.util.Util;
import com.sun.istack.Nullable;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Abstract AT.<br/>
 * By default expects at least 1 notification. <br/>
 * Does not contain {@link com.icegreen.greenmail.util.GreenMail} mail mock server.
 */
public abstract class AbstractMailAutomationTest extends CamelTestSupport {
  private final int httpPort = 8080;
  private String httpendpoint="/endpoint";
  private final String httpUrl = "http://127.0.0.1:"+ httpPort+httpendpoint;
  //for greenmail
  private final String login = "login-id", pwd = "password", to = "partsibprice@mail.ru";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(httpPort));

  ConfiguratorFactory configFactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
      //next line is to enter condition PricehookIdTaggingRulesLoaderProcessor:27
      kv.put("pricehook.config.url", "http://ANYTHING");
      kv.put("output.url", httpUrl);
      kv.put("email.enabled", "true");
      kv.put("local.enabled", "0");
      kv.put("ftp.enabled",   "0");
      kv.put("http.enabled",  "0");
      kv.put("endpoints.config.filename", "target/classes/test_local_endpoints0.json");
      kv.put("email.accept.rules.config.filename=", "src/main/resources/email_accept_rules.json");
      Map<String, String> configPairs = getConfig();
      if (configPairs != null)
        kv.putAll(configPairs);
    }
  };

  /**
   * Override this method to override default config values.
   * @return
   */
  public Map<String, String> getConfig() {
    return null;
  }

  Configurator config = configFactory.getConfigurator();

  MainRouteBuilder builder;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;
  @EndpointInject(uri = "mock:notification")
  protected MockEndpoint mockNotificationEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  private AttachmentVerifier attachmentVerifier;
  private int expectNumTotal;

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  public Map<String, DataHandler> makeAttachment(String name) {
    InputStream is = new ByteArrayInputStream("Hello Email World, yeah!".getBytes());
    return Collections.singletonMap(name, new DataHandler(is, "text/plain"));
  }

  /**
   * This impl removes real imap endpoint. Override to change. When override call setupHttpMock().
   * @param mockRouteName
   * @param mockAfterId
   */
  public void beforeLaunch(String mockRouteName, String mockAfterId) throws Exception {
    for (Endpoint e : getEmailEndpoints()) {
      //remove source imap endpoints:
      removeSourceEndpoints(e.id);
      setupNotificationMock(e.id);
    }
    setupHttpMock();
    setupDestinationMock(mockRouteName, mockAfterId);
  }

  private void removeSourceEndpoints(String endpointId) throws Exception {
    context.getRouteDefinition("source-" + endpointId).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        replaceFromWith("direct:none-"+endpointId);
      }
    });
  }

  public void setupNotificationMock(String endpointId) throws Exception {
    //mock notifications
    context.getRouteDefinition("notification-"+endpointId).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        //prevent real notification from being sent
        weaveById("notification-sender-"+endpointId).replace().to(mockNotificationEndpoint);
      }
    });
  }

  private void setupDestinationMock(final String route, final String id) throws Exception {
    context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        weaveById(id).after().to(mockEndpoint);
      }
    });
  }

  public void setupHttpMock() {
    //http mock endpoint setup
    stubFor(post(urlEqualTo(httpendpoint))
        .willReturn(aResponse()
            .withStatus(200)));
  }

  public AbstractMailAutomationTest setAttachmentVerifier(AttachmentVerifier verifier) {
    this.attachmentVerifier = verifier;
    return this;
  }

  public void launch(List<String> expectNames, List<String> expectTags, int expectNumTotal,
                     @Nullable Map<EmailMessage, String> msgEndpoints)
      throws Exception
  {
    launch("acceptedmail", "taglogger", expectTags, expectNames, expectNumTotal,
        msgEndpoints);
  }

  /**
   * Call this to start test.
   * More useful args list. Just proxy to another launch().
   */
  public void launch(String mockRouteName, String mockAfterId, List<String> expectTags, List<String> expectNames,
                     int expectNumTotal, String sendToEndpoint, @Nullable EmailMessage...msgs) throws Exception {
    HashMap<EmailMessage, String> map = null;
    if (msgs != null) {
      map = new HashMap<>();
      for (EmailMessage m : msgs)
        map.put(m, sendToEndpoint);
    }
    launch(mockRouteName, mockAfterId, expectTags, expectNames, expectNumTotal, map);
  }

  public void launch(String mockRouteName, String mockAfterId, List<String> expectTags, List<String> expectNames,
              int expectNumTotal, @Nullable Map<EmailMessage, String> msgEndpoints) throws Exception
  {
    beforeLaunch(mockRouteName, mockAfterId);

    if (expectNames != null && expectNumTotal != expectNames.size() ||
        expectTags != null && expectTags.size() != expectNumTotal)
      throw new IllegalArgumentException("Illegal arguments: must be same size of expected tags/names and number of messages");

    if (expectTags != null) mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(MainRouteBuilder.PRICEHOOK_ID_HEADER, expectTags.toArray());
    if (expectNames != null) mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, expectNames.toArray());
    this.expectNumTotal = expectNumTotal;
    mockEndpoint.expectedMessageCount(this.expectNumTotal);

    context.setTracing(isTracing());
    context.start();

    if (msgEndpoints != null)
      sendMessagesToEndpoints(msgEndpoints);
    waitBeforeAssert();
    assertConditions();
    log.info("Test PASSED: " + getClass().getSimpleName());

    context.stop();
  }

  /**
   * Override to modify assertion
   * @throws InterruptedException
   */
  public void assertConditions() throws Exception {
    log.info("Expecting {} messages", expectNumTotal);
    mockEndpoint.assertIsSatisfied();
    
    if (getExpectedNotificationCount() > 0) {
      log.info("Expecting " + getExpectedNotificationCount() + " notification(s)");
      mockNotificationEndpoint.expectedMessageCount(getExpectedNotificationCount());
    } else {
      log.info("Expecting at least one notification");
      mockNotificationEndpoint.expectedMinimumMessageCount(1);
    }
    mockNotificationEndpoint.assertIsSatisfied();
    //verify http mock endpoint
    WireMock.verify(
        WireMock.postRequestedFor(urlPathEqualTo(httpendpoint))
    );

    //verify attachments' names
    if (attachmentVerifier != null) {
      Map<String, InputStream> atts = new HashMap<>();
      List<LoggedRequest> reqs = WireMock.findAll(postRequestedFor(urlPathEqualTo(httpendpoint)));
      for (LoggedRequest req : reqs) {
        String fname = req.getHeader("X-Filename");
        atts.put(fname, new ByteArrayInputStream(req.getBody()));
      }

      assertTrue(attachmentVerifier.verify(atts));
    }
  }

  /**
   * Override to change expected number of notifications. This number is to be exact match.
   * @return exact number of notification to expect
   */
  public int getExpectedNotificationCount() {
    return 0;
  }

  /**
   * TODO: @shamil do we really need this? Since it's not from the parent class..
   * To be overriden for test.
   * @throws Exception
   */
  public abstract void test() throws Exception;

  /**
   * False by default. Override to
   * @return
   */
  public Boolean isTracing() {
    return false;
  }


  /**
   * Override to implement sleep or special wait before asserting results.
   */
  public void waitBeforeAssert() {
  }

  /**
   * Override this in subclasses to change default sending behaviour.
   * @param toSend messages list to send.
   */
  public void sendMessagesToEndpoints(Map<EmailMessage, String> toSend) {
    for (EmailMessage m : toSend.keySet()) {
      HashMap<String, Object> h = new HashMap() {{put("Subject", m.subject); put("From", m.from);}};
      template.send(toSend.get(m), exchange -> {
        exchange.getIn().setHeaders(h);
        for (String fname : m.attachments.keySet()) {
          exchange.getIn().addAttachment(fname, m.attachments.get(fname));
        }
      });
    }
  }

  public static Properties createNotificationsConfig(String period, String from, String to, String uri) {
    Properties p = new Properties();
    p.put("notification.period", period);
    p.put("email.from", from);
    p.put("email.to", to);
    p.put("email.uri", uri);
    return p;
  }

  public class RawEmailMessage extends EmailMessage {
    public RawEmailMessage(InputStream is) throws MessagingException, IOException {
      super(null);
      Session ses = Session.getDefaultInstance(new Properties());
      MimeMessage msg = new MimeMessage(ses, is);
      subject = msg.getSubject();
      attachments = new HashMap<>();
      from = msg.getFrom()[0].toString();
      String disposition = msg.getDisposition();
      if (disposition != null && disposition.contains("attachment")){
        //Extract attachment filename from headers
        String filename = msg.getFileName();
        //We need not only main header value, but parameters. They are not parsed my MimeMessage, so we do it here manually
        String[] headers = {"Content-Type", "Content-type", "Content-Disposition", "Content-disposition"};
        String[] params = {"name", "filename"};
        for (int i = 0; filename == null && i < headers.length; ++i){
          String[] headerValues = msg.getHeader(headers[i]);
          if (headerValues == null || headerValues.length == 0)
            continue;
          for (int z = 0; filename == null && z < headerValues.length; ++z) {
            for (int j = 0; filename == null && j < params.length; ++j) {
              if (headers[i].toLowerCase().contains("type")){
                try {
                  filename = new ContentType(headerValues[z]).getParameter(params[j]);
                }catch(ParseException e){
                  log.debug("cannot parse: header='"+headers[i]+"' val='"+headerValues[z]+"'", e);
                  continue;
                }
              } else {
                try {
                  filename = new ContentDisposition(headerValues[z]).getParameter(params[j]);
                }catch(ParseException e){
                  log.debug("cannot parse: header='"+headers[i]+"' val='"+headerValues[z]+"'", e);
                  continue;
                }
              }
            }
          }
        }
        filename = MimeUtility.decodeText(filename);
        if (filename != null) {
          DataHandler dh = new DataHandler(msg.getContent(), msg.getContentType());
          attachments.put(filename, dh);
        }
      } else {
        handleMessage(msg);
      }
    }

    public void handleMessage(javax.mail.Message message) throws IOException, MessagingException {
      Object content = message.getContent();
      String contentType = message.getContentType();
      if (content instanceof String ) {
        throw new RuntimeException("not yet impl");
      } else if (content instanceof Multipart) {
        Multipart mp = (Multipart) content;
        handleMultipart(mp);
      } else {
        throw new RuntimeException("not yet impl");
      }
    }

    public void handleMultipart(Multipart mp) throws MessagingException, IOException {
      int count = mp.getCount();
      for (int i = 0; i < count; i++) {
        BodyPart bp = mp.getBodyPart(i);
        Object content = bp.getContent();
        if (content instanceof String) {
          attachments.put(bp.getFileName(), new DataHandler(content, bp.getContentType()));
        } else if (content instanceof InputStream) {
          attachments.put(bp.getFileName(), new DataHandler(Util.readFully((InputStream) content),
              bp.getContentType()));
        } else if (content instanceof javax.mail.Message) {
          handleMessage((javax.mail.Message) content);
        } else if (content instanceof Multipart) {
          handleMultipart((Multipart) content);
        } else {
          log.error("Cannot process message content: class=" + content.getClass());
          throw new RuntimeException("not yet impl");
        }
      }
    }
  }

  public static class EmailMessage {
    String subject;
    Map<String, DataHandler> attachments;
    public String from;
    public Date date;

    public EmailMessage(String subject, String from, Date date, Map<String, DataHandler> attachments) {
      this(subject, from, attachments);
      this.date = date;
    }

    public EmailMessage(String subject, String from, Map<String, DataHandler> attachments) {
      this.subject = subject;
      this.from = from;
      this.attachments = attachments;
    }

    public EmailMessage(String subject, List<String> attachmentNames) {
      this.subject = subject;
      this.attachments = new HashMap<>();
      InputStream is = new ByteArrayInputStream(new byte[]{'1','2','3','4','5','6','7','8','9','0'});
      for (String fname : attachmentNames) {
        attachments.put(fname, new DataHandler(is, "text/plain"));
      }
    }

    public EmailMessage(String subject) {
      this.subject = subject;
    }
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    builder = new MainRouteBuilder(getConfigurator()){
      @Override
      public List<PricehookIdTaggingRule> getPricehookConfig() {
        return getTagRules();
      }

      @Override
      public PricehookIdTaggingRulesConfigLoaderProvider getConfigLoaderProvider() {
        return url -> getPricehookConfig();
      }

      @Override
      public ArrayList<EmailAcceptRule> getEmailAcceptRules() {
        return getAcceptRules();
      }

      @Override
      public List<Plugin> getPlugins() {
        return getPluginsList();
      }

      @Override
      public Endpoints getEndpoints() {
        Endpoints e = new Endpoints();
        e.ftp=new ArrayList<>();
        e.http=new ArrayList<>();
        e.email = new ArrayList<>();
        e.email.addAll(getEmailEndpoints());
        return e;
      }

      @Override
      public Properties loadNotificationConfig(String fname) {
        return AbstractMailAutomationTest.createNotificationsConfig(
            "3000", "partsibprice@yahoo.com",
            login,
            "smtp://127.0.0.1:3125?username="+login+"&password="+pwd+"&debugMode=true"
        );
      }
    };
    return builder;
  }

  protected List<Plugin> getPluginsList() {
    return null;
  }

  /**
   * Override this method to change config values.
   * @return config
   */
  protected Configurator getConfigurator() {
    return config;
  }

  /**
   * Override to create rules, this implementation accepts any letter with '@' in "From".
   */
  public ArrayList<EmailAcceptRule> getAcceptRules() {
    ArrayList<EmailAcceptRule> rules = new ArrayList<>();
    EmailAcceptRule r = new EmailAcceptRule();
    r.header="From";
    r.contains="@";
    rules.add(r);
    return rules;
  }

  public static List<PricehookIdTaggingRule> loadTagsFile(String filename) {
    try {
      String json = new String(Util.readFully(
          AbstractMailAutomationTest.class.getClassLoader().getResourceAsStream(filename)), "UTF-8");
      List<PricehookIdTaggingRule> list = MainRouteBuilder.parseTaggingRules(json);
      return list;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }



  /**
   * Override this if you use external server
   */
  public ArrayList<Endpoint> getEmailEndpoints(){
    Endpoint email = new Endpoint();
    email.id = "MockMailEndpoint";
    email.url = "imap.example.com";
    email.user = "email@a.com";
    email.pwd = "pwd";
    email.delay = "5000";
    return new ArrayList<Endpoint>(){{
      add(email);
    }};
  }

  public abstract List<PricehookIdTaggingRule> getTagRules();

  @Override
  protected int getShutdownTimeout() {
    return 60;
  }

  @Override
  public boolean isDumpRouteCoverage() {
    return true;
  }

  @Override
  protected boolean useJmx() {
    return true;
  }
}

