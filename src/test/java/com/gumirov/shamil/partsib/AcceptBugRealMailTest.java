package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

/**
 * TODO: make this abstract
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 6/6/2017 Time: 09:47<br/>
 */
@Ignore
public class AcceptBugRealMailTest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    super.launch("acceptedmail", "taglogger",
        Collections.singletonList("982.0.lamps"),
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
  public void beforeLaunch(String mockRouteName, String mockAfterId) {
    //super.beforeLaunch();
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("tagrules.json");
  }

  @Override
  public ArrayList<EmailAcceptRule> getAcceptRules() {
    return loadEmailAcceptRules("13jan_email_accept.json");
  }

  //todo move to parent
  public ArrayList<EmailAcceptRule> loadEmailAcceptRules(String filename){
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(filename), "UTF-8");
      return mapper.readValue(json, new TypeReference<List<EmailAcceptRule>>(){});
    } catch (IOException e) {
      return new ArrayList<>();
    }
  }

  @Override
  public Endpoint getEmailEndpoint() {
    Endpoint endp = new Endpoint();
    Properties p = new Properties();
    try {
      p.load(getClass().getClassLoader().getResourceAsStream("gmail_password.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    endp.id = getEndpointName();
    endp.pwd = p.getProperty("pwd");
    endp.user = p.getProperty("user");
    endp.url = p.getProperty("host");
    endp.delay = "50000";
    return endp;
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
