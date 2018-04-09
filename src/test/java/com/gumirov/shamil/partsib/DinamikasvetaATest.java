package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * NullPointerException in log is expected for this test.
 *
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class DinamikasvetaATest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Collections.singletonList("982.0.lamps"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("13jan0.txt"))
    );
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("tagrules.json");
  }
}
