package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.*;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import javax.activation.DataHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 *
 */
public class PricehookTagFilterUnitTest extends CamelTestSupport {
  private static final String ENDPID = "Test-EMAIL-01";
  ConfiguratorFactory cfactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
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

  @Test
  public void test() throws Exception{
    context.getRouteDefinition("acceptedmail").adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
        weaveById("PricehookTagFilter").after().to(mockEndpoint);
      }
    });

    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(MainRouteBuilder.PRICEHOOK_ID_HEADER,
        Arrays.asList("quotedSupplier", "goodSupplier"));
    mockEndpoint.expectedMessageCount(2);

    context.setTracing(true);
    context.start();
    //to be passed:
    template.sendBodyAndHeader("direct:emailreceived", "", "Subject", "\"quoted\" string");
    template.sendBodyAndHeader("direct:emailreceived", "", "From", "good");
    //to be rejected
    template.sendBodyAndHeader("direct:emailreceived", "", "Subject", "to be rejected");
    template.sendBodyAndHeader("direct:emailreceived", "", "Subject", "to be \"rejected\"");

    mockEndpoint.assertIsSatisfied();
    context.stop();
  }

  /**
   * This is test for separate tagging of attachments.
   * @throws Exception
   */
  @Test
  public void testSeparateAttachmentTags() throws Exception{
    context.getRouteDefinition("acceptedmail").adviceWith(context, new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
        weaveById("taglogger").after().to(mockEndpoint);
      }
    });
    List<String> tags = Arrays.asList("filerule_1_1", "filerule_1_2", "filerule_2_2", "goodSupplier");
    List<String> fnames = Arrays.asList("filerule_1.csv", "filerule_2.csv", "filerule_2.csv", "goodSupplier.csv");

    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(MainRouteBuilder.PRICEHOOK_ID_HEADER, tags.toArray());
//    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, fnames.toArray());
    mockEndpoint.expectedMessageCount(4);

    context.setTracing(true);
    context.start();
    //to be changed by attachment tagger:
    HashMap<String, Object> headers1 = new HashMap<String, Object>(){{
      put("Subject", "\"quoted\" string");
      put("From", "first");
    }};
    InputStream is = new ByteArrayInputStream(new byte[]{'1','2','3','4','5','6','7','8','9','0'});
    template.send("direct:emailreceived", exchange -> {
      exchange.getIn().setHeaders(headers1);
      exchange.getIn().addAttachment(fnames.get(0), new DataHandler(is, "text/plain"));
    });
    template.send("direct:emailreceived", exchange -> {
      exchange.getIn().setHeaders(headers1);
      exchange.getIn().addAttachment(fnames.get(1), new DataHandler(is, "text/plain"));
    });
    HashMap<String, Object> headers2 = new HashMap<String, Object>(){{
      put("From", "good");
      put("Subject", "some subject");
    }};
    template.send("direct:emailreceived", exchange -> {
      exchange.getIn().setHeaders(headers2);
      exchange.getIn().addAttachment(fnames.get(2), new DataHandler(is, "text/plain"));
    });
    template.send("direct:emailreceived", exchange -> {
      exchange.getIn().setHeaders(headers2);
      exchange.getIn().addAttachment(fnames.get(3), new DataHandler(is, "text/plain"));
    });

    mockEndpoint.assertIsSatisfied();
    context.stop();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    builder = new MainRouteBuilder(config){
      @Override
      public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
        PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
        PricehookIdTaggingRule r2 = new PricehookIdTaggingRule();
        r1.header = "Subject";
        r1.contains = "\"quoted\"";
        r1.pricehookid = "quotedSupplier";
        r1.filerules = Arrays.asList(
            new AttachmentTaggingRule("1", "filerule_1_1"),
            new AttachmentTaggingRule("2", "filerule_1_2")
        );
        r2.header = "From";
        r2.contains = "good";
        r2.pricehookid = "goodSupplier";
        r2.filerules = Arrays.asList(
            new AttachmentTaggingRule("2", "filerule_2_2")
        );
        return Arrays.asList(r1, r2);
      }

      @Override
      public ArrayList<EmailAcceptRule> getEmailAcceptRules() throws IOException {
        ArrayList<EmailAcceptRule> rules = new ArrayList<>();
        EmailAcceptRule r1 = new EmailAcceptRule();
        r1.header="Subject";
        r1.contains="\"quoted\"";
        EmailAcceptRule r2 = new EmailAcceptRule();
        r2.header="From";
        r2.contains="good";
        rules.add(r1);
        rules.add(r2);
        return rules;
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
        e.email = new ArrayList<Endpoint>();
        //imaps://imap.mail.ru?password=gfhjkm12&username=sh.roller%40mail.ru&consumer.delay=10000&delete=false&fetchSize=1").
        Endpoint email = new Endpoint();
        email.id=ENDPID;
        email.url= "imap.example.com";
        email.user="email@a.com";
        email.pwd="pwd";
        email.delay="5000";
        e.email.add(email);
        return e;
      }
    };
    return builder;
  }

  @Override
  protected int getShutdownTimeout() {
    return 60;
  }

  @Override
  public boolean isDumpRouteCoverage() {
    return true;
  }
}
