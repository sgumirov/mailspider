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
public class YahooRawMailTest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    super.launch("acceptedmail", "taglogger",
        Arrays.asList("977.0.msk"),
        null, 1, "direct:emailreceived",
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("double_quotes_bad.eml"))
    );
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
}
