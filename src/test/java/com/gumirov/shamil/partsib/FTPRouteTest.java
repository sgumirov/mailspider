package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Automation FTP endpoint test with local FTP daemon
 */
@Ignore("not a UT")
public class FTPRouteTest extends CamelTestSupport {

//  static final String ftpDir = "/opt/ftp/files";
  static final String ftpDir = "/tmp/files";
  static final String resDir = "src/data/test";
  static final String url = "http://127.0.0.1/1.php";
//  private static final String FTPURL = "ftp://127.0.0.1:2021/files/";
  private static final String FTPURL = "ftp://127.0.0.1/files/";

  ConfiguratorFactory cfactory = new ConfiguratorFactory(){
    @Override
    protected void initDefaultValues(HashMap<String, String> kv) {
      super.initDefaultValues(kv);
      kv.put("email.enabled", "0");
      kv.put("local.enabled", "0");
      kv.put("ftp.enabled",   "1");
      kv.put("http.enabled",  "0");
      kv.put("output.url", url);
      kv.put("endpoints.config.filename", "target/classes/test_local_endpoints.json");
    }
  };
  Configurator config = cfactory.getConfigurator();

  MainRouteBuilder builder;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;

  @Before
  public void setupFTP() throws IOException {
    FileUtils.deleteDirectory(new File(ftpDir));
    FileUtils.copyDirectory(new File(resDir), new File(ftpDir));
    
    //clear:
    new File(config.get("idempotent.repo")).delete();

    AdviceWithRouteBuilder mockresult = new AdviceWithRouteBuilder() {

      @Override
      public void configure() {
        // mock the for testing
        weaveById("outputprocessor").replace().to(mockEndpoint);
      }
    };
    try {
      context.getRouteDefinition("output1").adviceWith(context, mockresult);
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
    mockEndpoint.expectedMessageCount(3);
    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, "plaintext.txt",
        "zip2.txt", "ziptxt.txt");
    context.setTracing(true);
    context.setMessageHistory(true);
    context.start();
    mockEndpoint.assertIsSatisfied();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    builder = new MainRouteBuilder(config){
      @Override
      public Endpoints getEndpoints() {
        Endpoints e = new Endpoints();
        e.ftp = new ArrayList<Endpoint>();
        Endpoint ftp = new Endpoint();
        ftp.id="Test-FTP-01";
        ftp.url=FTPURL;
        ftp.user="anonymous";
        ftp.pwd="a@b.com";
        ftp.delay="6000";
        e.ftp.add(ftp);
        e.email=new ArrayList<>();
        e.http=new ArrayList<>();
        return e;
      }
    };
    return builder; 
  }
}
