package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.*;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoaderProvider;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;
import org.junit.Test;

import javax.activation.DataHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Properties;

/**
 *
 */
public class PricehookTagFilterUnitTest extends CamelTestSupport {
  private static final String ENDPID = "PricehookTest-EMAIL-01";
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

  private final String login = "login-id", pwd = "password", to = "partsibprice@mail.ru";
  @Rule
  public final GreenMailRule notificationsSmtp = new GreenMailRule(new ServerSetup(3125, "127.0.0.1", "smtp"));

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Arrays.asList("quotedSupplier", "goodSupplier"), null, 2,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().email.get(0)), //send through first endpoint
        new EmailMessage("\"quoted\" string", Collections.singletonList("a")),
        new EmailMessage("good", Collections.singletonList("b")),
        new EmailMessage("to be rejected", Collections.singletonList("b")),
        new EmailMessage("to be \"rejected\"", Collections.singletonList("b"))
    );
  }

  @Test
  public void testDoubleQuotesRulesPositive() throws Exception {
    launch("acceptedmail", "taglogger",
        Collections.singletonList("master.nsk"), Collections.singletonList("a"),
        1,
        new HashMap<EmailMessage, String>(){{
          //right: tests double space
          put(new EmailMessage("Прайс-лист ООО \"Мастер    Сервис\"  наличие Новосибирск",
              Collections.singletonList("a")),
              EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().email.get(0)) //send through first endpoint
          );
        }}
    );
  }

  @Test
  public void testDoubleQuotesRulesNegative() throws Exception {
    //test negative: no quotes
    launch("acceptedmail", "taglogger",
        new ArrayList<>(), new ArrayList<>(),
        0,
        new HashMap<EmailMessage, String>(){{
          put(new EmailMessage("Прайс-лист ООО 'Мастер Сервис' наличие Новосибирск",
              Collections.singletonList("b")),
              EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().email.get(0))
          );
        }}
    );
  }

  public void launch(String route, String id, List<String> expectTags, List<String> expectNames,
              int expectNumTotal, String sendToEndpoint, EmailMessage...msgs) throws Exception {
    HashMap<EmailMessage, String> map = new HashMap<>();
    for (EmailMessage m : msgs)
      map.put(m, sendToEndpoint);
    launch(route, id, expectTags, expectNames, expectNumTotal, map);
  }

  /**
   * This impl removed real imap endpoint. Override to change.
   */
  public void beforeLaunch() throws Exception {
    notificationsSmtp.reset();
    notificationsSmtp.setUser(login, pwd);
    //remove imap endpoint
    context.getRouteDefinition("source-"+ENDPID).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        replaceFromWith("direct:none");
      }
    });
  }

  public void launch(String route, String id, List<String> expectTags, List<String> expectNames,
              int expectNumTotal, Map<EmailMessage, String> toSend) throws Exception {
    context.getRouteDefinition(route).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
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

    context.setTracing(true);
    context.start();

    sendMessages(context, toSend);

    log.info("Expecting {} messages", expectNumTotal);
    mockEndpoint.assertIsSatisfied();
    context.stop();
  }

  /**
   * Override this in subclasses to change default sending behaviour.
   * @param context camel context
   * @param toSend messages list to send.
   */
  public void sendMessages(CamelContext context, Map<EmailMessage, String> toSend) {
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

  class EmailMessage {
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

  /**
   * This is test for separate tagging of attachments.
   * @throws Exception
   */
  @Test
  public void testSeparateAttachmentTags() throws Exception{
    context.getRouteDefinition("acceptedmail").adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        weaveById("taglogger").after().to(mockEndpoint);
      }
    });
    context.getRouteDefinition("source-"+ENDPID).adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() {
        replaceFromWith("direct:none");
      }
    });
    List<String> tags = Arrays.asList("filerule_1_1", "filerule_1_2", "filerule_2_2", "goodSupplier");
    List<String> fnames = Arrays.asList("filerule_1.csv", "filerule_2.csv", "filerule_2.csv", "goodSupplier.csv");

    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(MainRouteBuilder.PRICEHOOK_ID_HEADER, tags.toArray());
    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, fnames.toArray());
    mockEndpoint.expectedMessageCount(4);

    context.setTracing(true);
    context.start();

    //attachments filename tagging:
    //to be changed by attachment tagger:
    HashMap<String, Object> headers1 = new HashMap<String, Object>(){{
      put("Subject", "\"quoted\" string");
      put("From", "@");
    }};
    InputStream is = new ByteArrayInputStream(new byte[]{'1','2','3','4','5','6','7','8','9','0'});

    //send through first endpoint
    String endpointUri = EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().email.get(0));
    template.send(endpointUri, exchange -> {
      exchange.getIn().setHeaders(headers1);
      exchange.getIn().addAttachment(fnames.get(0), new DataHandler(is, "text/plain"));
    });
    template.send(endpointUri, exchange -> {
      exchange.getIn().setHeaders(headers1);
      exchange.getIn().addAttachment(fnames.get(1), new DataHandler(is, "text/plain"));
    });

    HashMap<String, Object> headers2 = new HashMap<String, Object>(){{
      put("From", "@");
      put("Subject", "good");
    }};
    template.send(endpointUri, exchange -> {
      exchange.getIn().setHeaders(headers2);
      exchange.getIn().addAttachment(fnames.get(2), new DataHandler(is, "text/plain"));
    });
    template.send(endpointUri, exchange -> {
      exchange.getIn().setHeaders(headers2);
      exchange.getIn().addAttachment(fnames.get(3), new DataHandler(is, "text/plain"));
    });

    mockEndpoint.assertIsSatisfied();
    context.stop();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    builder = new MainRouteBuilder(config){
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
        ArrayList<EmailAcceptRule> rules = new ArrayList<>();
        EmailAcceptRule r1 = new EmailAcceptRule();
        r1.header="Subject";
        r1.contains="\"quoted\"";
        EmailAcceptRule r2 = new EmailAcceptRule();
        r2.header="From";
        r2.contains="@";
//        rules.add(r1);
        rules.add(r2);
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
      public List<Plugin> getPlugins() {
        return null;
      }

      @Override
      public Endpoints getEndpoints() {
        return getEmailEndpoints();
      }
    };
    return builder;
  }

  protected Endpoints getEmailEndpoints() {
    Endpoints e = new Endpoints();
    e.ftp = new ArrayList<>();
    e.http = new ArrayList<>();
    e.email = new ArrayList<>();
    //imaps://imap.mail.ru?password=gfhjkm12&username=sh.roller%40mail.ru&consumer.delay=10000&delete=false&fetchSize=1").
    Endpoint email = new Endpoint();
    email.id = ENDPID;
    email.url = "imap.example.com";
    email.user = "email@a.com";
    email.pwd = "pwd";
    email.delay = "5000";
    e.email.add(email);
    return e;
  }

  public List<PricehookIdTaggingRule> getTagRules() {
    PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
    PricehookIdTaggingRule r2 = new PricehookIdTaggingRule();
    PricehookIdTaggingRule r3 = new PricehookIdTaggingRule();
    r1.header = "Subject";
    r1.contains = "\"quoted\"";
    r1.pricehookid = "quotedSupplier";
    r1.filerules = Arrays.asList(
        new AttachmentTaggingRule("1", "filerule_1_1"),
        new AttachmentTaggingRule("2", "filerule_1_2")
    );
    r2.header = "Subject";
    r2.contains = "good";
    r2.pricehookid = "goodSupplier";
    r2.filerules = Collections.singletonList(
        new AttachmentTaggingRule("2", "filerule_2_2")
    );
    r3.header = "Subject";
    r3.contains = "Прайс-лист ООО \"Мастер Сервис\" наличие Новосибирск";
    r3.pricehookid = "master.nsk";
    return Arrays.asList(r1, r2, r3);
  }

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
