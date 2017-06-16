package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.*;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoaderProvider;
import com.gumirov.shamil.partsib.util.Util;
import com.sun.istack.Nullable;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Properties;

/**
 * Abstract AT.
 */
public abstract class AbstractMailAutomationTest extends CamelTestSupport {
  private static final String ENDPID = "Test-EMAIL-01";

  ConfiguratorFactory cfactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
      //next line is to enter condition PricehookIdTaggingRulesLoaderProcessor:27
      kv.put("pricehook.config.url", "http://ANYTHING");
      kv.put("email.enabled", "true");
      kv.put("local.enabled", "0");
      kv.put("ftp.enabled",   "0");
      kv.put("http.enabled",  "0");
      kv.put("endpoints.config.filename", "target/classes/test_local_endpoints.json");
      kv.put("email.accept.rules.config.filename=", "src/main/resources/email_accept_rules.json");
    }
  };
  Configurator config = cfactory.getConfigurator();

  MainRouteBuilder builder;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  /**
   * This impl removes real imap endpoint. Override to change.
   */
  public void beforeLaunch() throws Exception {
    //remove imap endpoint
    context.getRouteDefinition("source-"+getEndpointName()).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
        replaceFromWith("direct:none");
      }
    });
  }

  /**
   * Call this to start test.
   * More useful args list. Just proxy to another launch().
   */
  public void launch(String route, String id, List<String> expectTags, List<String> expectNames,
                     int expectNumTotal, String sendToEndpoint, @Nullable EmailMessage...msgs) throws Exception {
    HashMap<EmailMessage, String> map = null;
    if (msgs != null) {
      map = new HashMap<>();
      for (EmailMessage m : msgs)
        map.put(m, sendToEndpoint);
    }
    launch(route, id, expectTags, expectNames, expectNumTotal, map);
  }

  public void launch(String route, String id, List<String> expectTags, List<String> expectNames,
              int expectNumTotal, @Nullable Map<EmailMessage, String> toSend) throws Exception {
    context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
        weaveById(id).after().to(mockEndpoint);
      }
    });

    beforeLaunch();

    if (expectNames != null && expectNumTotal != expectNames.size() ||
        expectTags != null && expectTags.size() != expectNumTotal)
      throw new IllegalArgumentException("Illegal arguments: must be same size of expected tags/names and number of messages");

    if (expectTags != null) mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(MainRouteBuilder.PRICEHOOK_ID_HEADER, expectTags.toArray());
    if (expectNames != null) mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, expectNames.toArray());
    mockEndpoint.expectedMessageCount(expectNumTotal);

    context.setTracing(isTracing());
    context.start();

    if (toSend != null) sendMessages(toSend);

    waitForCompletion();

    log.info("Expecting {} messages", expectNumTotal);
    mockEndpoint.assertIsSatisfied();
    context.stop();
  }

  public Boolean isTracing() {
    return true;
  }


  /**
   * Override to implement sleep before asserting results.
   */
  public void waitForCompletion() {
  }

  /**
   * Override this in subclasses to change default sending behaviour.
   * @param toSend messages list to send.
   */
  public void sendMessages(Map<EmailMessage, String> toSend) {
    for (EmailMessage m : toSend.keySet()) {
      HashMap h = new HashMap(){{put("Subject", m.subject); put("From", "@");}};
      template.send(toSend.get(m), exchange -> {
        exchange.getIn().setHeaders(h);
        for (String fname : m.attachments.keySet()) {
          exchange.getIn().addAttachment(fname, m.attachments.get(fname));
        }
      });
    }
  }

  public class RawEmailMessage extends EmailMessage {
    public RawEmailMessage(InputStream is) throws MessagingException, IOException {
      super(null);
      Session ses = Session.getDefaultInstance(new Properties());
      MimeMessage msg = new MimeMessage(ses, is);
      this.subject = msg.getSubject();
      this.attachments = new HashMap<>();
      handleMessage(msg);
    }

    public void handleMessage(javax.mail.Message message) throws IOException, MessagingException {
      Object content = message.getContent();
      if (content instanceof String) {
//        attachments.put(bp.getFileName(), new DataHandler(content, "text/plain"));
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

  public class EmailMessage {
    String subject;
    Map<String, DataHandler> attachments;

    public EmailMessage(String subject, Map<String, DataHandler> attachments) {
      this.subject = subject;
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
  protected RoutesBuilder createRouteBuilder() throws Exception {
    builder = new MainRouteBuilder(config){
      @Override
      public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
        return getTagRules();
      }

      @Override
      public PricehookIdTaggingRulesConfigLoaderProvider getConfigLoaderProvider() {
        return url -> getPricehookConfig();
      }

      @Override
      public ArrayList<EmailAcceptRule> getEmailAcceptRules() throws IOException {
        return getAcceptRules();
      }

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
        Endpoint email = getEndpoint();
        e.email.add(email);
        return e;
      }
    };
    return builder;
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

  /**
   * Override this if you use external server
   */
  public Endpoint getEndpoint(){
    Endpoint email = new Endpoint();
    email.id=getEndpointName();
    email.url= "imap.example.com";
    email.user="email@a.com";
    email.pwd="pwd";
    email.delay="5000";
    return email;
  }

  /**
   * Use this method's return value when redefining route or override this method.
   */
  public String getEndpointName() {
    return ENDPID;
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
