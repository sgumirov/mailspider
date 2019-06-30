package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.*;
import com.partsib.mailspider.plugins.ExcelToCsvConverterPlugin;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
//disabled test as mock httpd crashes (returns 500 for internal status url) - need to replace it at least for this test.
@Ignore
public class AvtotrialATest extends AbstractMailAutomationTest {
  @Test
  public void test() throws Exception {
    //this email has large attachment (~20mb)
    RawEmailMessage email = new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("avtotrial/original_msg-4.txt"));
    launch("acceptedmail", "taglogger",
        Collections.singletonList("998.0.main"),
        Collections.singletonList("Оптовый прайс + под заказ - Цена опт - Рассылки.xlsx"),
        1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        email
    );
  }

  @Override
  protected List<Plugin> getPluginsList() {
    return Collections.singletonList(new ExcelToCsvConverterPlugin());
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("prod_rules.json");
  }
}
