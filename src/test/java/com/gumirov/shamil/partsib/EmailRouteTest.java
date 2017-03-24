package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Automation FTP endpoint test with local FTP daemon
 */
@Ignore("not a UT")
public class EmailRouteTest extends CamelTestSupport {

//  static final String ftpDir = "/opt/ftp/files";
  static final String ftpDir = "/tmp/files";
  static final String resDir = "src/data/test";
  static final String url = "http://im.mad.gd/2.php";
//  private static final String EMAIL_URL = "ftp://127.0.0.1:2021/files/";
  private static final String EMAIL_URL = "imap.mail.ru";

  ConfiguratorFactory cfactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
      kv.put("email.enabled", "true");
      kv.put("local.enabled", "0");
      kv.put("ftp.enabled",   "0");
      kv.put("http.enabled",  "0");
      kv.put("output.url", url);
      kv.put("endpoints.config.filename", "target/classes/test_local_endpoints.json");
      kv.put("email.rules.config.filename=", "src/main/resources/email_reject_rules.json");
    }
  };
  Configurator config = cfactory.getConfigurator();


  MailSpiderRouteBuilder builder;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;

  @Before
  public void setupFTP() throws IOException {
/*
    FileUtils.deleteDirectory(new File(ftpDir));
    FileUtils.copyDirectory(new File(resDir), new File(ftpDir));
*/

    //clear:
    new File(config.get("email.idempotent.repo")).delete();


//mock output for test:
    AdviceWithRouteBuilder mockresult = new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
        weaveById("outputprocessor").replace().to(mockEndpoint);
      }
    };
    try {
      context.getRouteDefinition("output").adviceWith(context, mockresult);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean isUseAdviceWith() {
    return true;
  }

  @Test
  @Ignore("not a UT")
  public void test() throws Exception{
    mockEndpoint.expectedMessageCount(1);
    mockEndpoint.setResultWaitTime(60000);
    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, "plaintext.txt", "zip2.txt", "ziptxt.txt", "rarfile2.txt", "rartxt.txt", "1.txt");
    context.setTracing(true);
    context.setMessageHistory(true);
    context.start();
//    Thread.sleep(10000);
    mockEndpoint.assertIsSatisfied();
  }

  @Override
  protected int getShutdownTimeout() {
    return 60;
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    builder = new MailSpiderRouteBuilder(config){
      @Override
      public Endpoints getEndpoints() throws IOException {
        Endpoints e = new Endpoints();
        e.ftp=new ArrayList<>();
        e.http=new ArrayList<>();
        e.email = new ArrayList<Endpoint>();
        //imaps://imap.mail.ru?password=gfhjkm12&username=sh.roller%40mail.ru&consumer.delay=10000&delete=false&fetchSize=1").
        Endpoint email = new Endpoint();
        email.id="Test-EMAIL-01";
        email.url= EMAIL_URL;
        email.user="sh.roller@mail.ru";
        email.pwd="gfhjkm12";
        email.delay="5000";
        e.email.add(email);
        return e;
      }

      @Override
      public ArrayList<EmailRule> getEmailRules() throws IOException {
        ArrayList<EmailRule> rules = new ArrayList<>();
        EmailRule r1 = new EmailRule();
        r1.header="Subject";
        r1.contains="bad";
        EmailRule r2 = new EmailRule();
        r2.header="Subject";
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
        r1.contains = "good";
        r1.pricehookid = "good-supplier";
        return Arrays.asList(r1, r2);
      }
    };
    return builder; 
  }
}
