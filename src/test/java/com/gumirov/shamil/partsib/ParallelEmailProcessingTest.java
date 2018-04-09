package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.GreenMailUtils;
import com.icegreen.greenmail.junit.GreenMailRule;
import org.junit.Ignore;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class ParallelEmailProcessingTest extends AbstractMailAutomationTest {
  protected GreenMailUtils utils = new GreenMailUtils();

  @Rule
  private final GreenMailRule greenMail1 = utils.getGreenMailRules(2);

  @Override
  public void test() {
    //todo assert overall number of output files
    launch();
  }

  @Override
  public ArrayList<Endpoint> getEmailEndpoints() {
    return utils.getGreenmailEndpoints(2);
  }

  private final String TAG = "TAG";
  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG;
    rule.id = "id";
    rule.header = "From";
    rule.contains = "@";
    rule.filerules = new ArrayList<AttachmentTaggingRule>(){{
      add(new AttachmentTaggingRule("1", "filerule_1_1"));
      add(new AttachmentTaggingRule("2", "filerule_1_2"));
    }};
    return Collections.singletonList(rule);
  }
}

