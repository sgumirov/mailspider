package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.JsonParser;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 6/6/2017 Time: 09:47<br/>
 */
@Ignore
public class AcceptBugRealMailTest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    super.launch("acceptedmail", "taglogger",
        Collections.singletonList("982.0.lamps"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0))//send through first endpoint
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
  public void beforeLaunch(String mockRouteName, String mockAfterId) {
    //super.beforeLaunch();
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("tagrules.json");
  }

  @Override
  public List<EmailAcceptRule> getAcceptRules() {
    return loadEmailAcceptRules("13jan_email_accept.json");
  }

  //todo move to parent
  public List<EmailAcceptRule> loadEmailAcceptRules(String filename){
    try {
      return Arrays.asList(new JsonParser<EmailAcceptRule[]>().load(filename, EmailAcceptRule[].class, "UTF-8"));
    } catch (IOException e) {
      return new ArrayList<>();
    }
  }

  @Override
  public List<Endpoint> getEmailEndpoints() {
    Endpoint endp = new Endpoint();
    Properties p = new Properties();
    try {
      p.load(getClass().getClassLoader().getResourceAsStream("gmail_password.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    endp.id = p.getProperty("id");
    endp.pwd = p.getProperty("pwd");
    endp.user = p.getProperty("user");
    endp.url = p.getProperty("host");
    endp.delay = "50000";
    return new ArrayList<Endpoint>(){{
      add(endp);
    }};
  }

  @Override
  public void waitBeforeAssert() {
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
