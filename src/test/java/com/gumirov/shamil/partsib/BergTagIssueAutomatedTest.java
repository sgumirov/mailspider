package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.AttachmentVerifier;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.RawEmailMessage;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * Test for BERG wrong tag issue.
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class BergTagIssueAutomatedTest extends AbstractMailAutomationTest {
  private String[] rawEmailFiles = new String[]{
      "original_msg.txt",
      "original_msg-2.txt",
      "original_msg-3.txt",
  };
  private final static String folder = "BergTagIssue";

  private HashMap<String, String> expectTags = new HashMap<String, String>(){{
    put("BERG_764_20180423_190056.csv", "764.0.main1");
    put("BERG_764_20180423_040107.csv", "764.0.main2");
    put("BERG_764_20180423_190040.csv", "977.0.nsk.2");
  }};

  @Override
  @Test
  public void test() throws Exception {
    List<EmailMessage> msgs = new ArrayList<>();
    for (String fileName : rawEmailFiles) {
      fileName = folder + File.separatorChar + fileName;
      try {
        msgs.add(new RawEmailMessage(getClass().getClassLoader().getResource(fileName).openStream()));
      } catch (Exception e) {
        log.error("Cannot read file: "+fileName, e);
        assertTrue("Cannot read "+fileName, false);
      }
    }
    List<String> names = new ArrayList<>();
    for (EmailMessage m : msgs)
      for (String name : m.attachments.keySet())
        names.add(name);
    ArrayList<Endpoint> endpoints = getEmailEndpoints();
    final String ENDP1 = EndpointSpecificUrl.apply("direct:emailreceived", endpoints.get(0));
    Map<EmailMessage, String> sendTo = new HashMap<>();
    for (EmailMessage m : msgs) sendTo.put(m, ENDP1);
    setAttachmentVerifier(new AttachmentVerifier() {
      @Override
      public boolean verifyTags(Map<String, String> attachments) {
        if (attachments.size() != expectTags.size()) return false;
        for (String name: attachments.keySet()) {
          if (!attachments.get(name).equals(expectTags.get(name)))
            return false;
        }
        return true;
      }
    });
    launch(names, getExpectTags(names), names.size() , sendTo);
  }

  private List<String> getExpectTags(List<String> filenames) {
    ArrayList<String> tags = new ArrayList<>();
      tags.add("764.0.main2");
      tags.add("764.0.main1");
      tags.add("977.0.nsk.2");
    return tags;
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    ArrayList<PricehookIdTaggingRule> rules = new ArrayList<>(loadTagRules("prod_tagrules.json"));
    //add rule berg новосиб
    rules.addAll(loadTagRules("berg_novosib_rule_disabled_in_prod.json"));
    return rules;
  }
}
