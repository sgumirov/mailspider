package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.IOException;
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

  @Override
  protected int getShutdownTimeout() {
    return 60;
  }

  @Override
  public boolean isDumpRouteCoverage() {
    return true;
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    builder = new MainRouteBuilder(config){
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
      public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
        PricehookIdTaggingRule r1 = new PricehookIdTaggingRule();
        PricehookIdTaggingRule r2 = new PricehookIdTaggingRule();
        r1.header = "Subject";
        r1.contains = "\"quoted\"";
        r1.pricehookid = "quotedSupplier";
        r2.header = "From";
        r2.contains = "good";
        r2.pricehookid = "goodSupplier";
        return Arrays.asList(r1, r2); 
      }
    };
    return builder;
  }
}
