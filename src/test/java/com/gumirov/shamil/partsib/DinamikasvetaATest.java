package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class DinamikasvetaATest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Collections.singletonList("982.0.lamps"),
        null, 1, "direct:emailreceived",
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("13jan0.txt"))
    );
  }

  //manual real email test
  public void testRaw() throws Exception {

  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("tagrules.json");
  }
}
