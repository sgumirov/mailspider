package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.RawEmailMessage;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class Kumir54ATest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Arrays.asList("11969.0.main"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("kumir54.eml"))
    );
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("prod_rules.json");
  }
}
