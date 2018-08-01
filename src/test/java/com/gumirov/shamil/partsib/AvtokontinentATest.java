package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.junit.Ignore;

import java.util.List;

/**
 * Warning: current (01.08.18) set of accept rules does not have a rule for avtokontinent. So ignoring this test for
 * now.
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
@Ignore
public class AvtokontinentATest extends AbstractMailAutomationTest {

  private final String rawEmail = "avtokontinent/autokontinent.txt";

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagRules("avtokontinent/prod_rules_01_08_18.json");
  }
}
