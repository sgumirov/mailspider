package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 6/6/2017 Time: 09:47<br/>
 */
@Ignore
public class YahooRealMailTest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    super.launch("acceptedmail", "taglogger",
        Arrays.asList("977.0.msk"),
        null, 1, "direct:emailreceived",
        null
    );
  }

  @Override
  public Boolean isTracing() {
    return false;
  }

  @Override
  protected int getShutdownTimeout() {
    return 120;
  }

  @Override
  public void beforeLaunch() throws Exception {
    //super.beforeLaunch();
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    try {
      String json = new String(Util.readFully(
          getClass().getClassLoader().getResourceAsStream("partsib_tags_config2.json")), "UTF-8");
      List<PricehookIdTaggingRule> list = MainRouteBuilder.parseTaggingRules(json);
      return list;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public ArrayList<EmailAcceptRule> getAcceptRules() {
    ArrayList<EmailAcceptRule> list = super.getAcceptRules();
    list.get(0).contains = "yahoo.com";
    return list;
  }

  @Override
  public Endpoint getEndpoint() {
    Endpoint endp = new Endpoint();
    endp.id=getEndpointName();
    Properties p = new Properties();
    try {
      p.load(getClass().getClassLoader().getResourceAsStream("test_mail.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    endp.pwd = p.getProperty("pwd");
    endp.user = p.getProperty("user");
    endp.url = p.getProperty("host");
    endp.delay = "50000";
    return endp;
  }

  @Override
  public void waitForCompletion() {
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
