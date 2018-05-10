package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;

import java.util.*;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class SpecialCharsFilteringAutomatedTest extends AbstractMailAutomationTest {
  //pricehook id
  private static final String TAG = "TAG";
  //file tags
  private static final String TAG1 = "TAG1";
  private static final String TAG2 = "TAG2";

  @Override
  @Test
  public void test() throws Exception {
    List<EmailMessage> msgs = createMessages();
    List<String> names = new ArrayList<>();
    for (EmailMessage m : msgs)
      for (String name : m.attachments.keySet())
        names.add(name);
    ArrayList<Endpoint> endpoints = getEmailEndpoints();
    final String ENDP1 = EndpointSpecificUrl.apply("direct:emailreceived", endpoints.get(0));
    Map<EmailMessage, String> sendTo = new HashMap<>();
    sendTo.put(msgs.get(0), ENDP1);
    sendTo.put(msgs.get(1), ENDP1);
    launch(names, getExpectTags(names), names.size(), sendTo);
  }

  private List<String> getExpectTags(List<String> filenames) {
    ArrayList<String> tags = new ArrayList<>();
    filenames.forEach(s -> {
      if (s.contains("1")) tags.add(TAG1);
      if (s.contains("2")) tags.add(TAG2);
    });
    return tags;
  }

  private List<EmailMessage> createMessages() {
    ArrayList<EmailMessage> msgs = new ArrayList<>();
    msgs.add(new EmailMessage("\"Москва\"", "no@ivers.ru",
        new Date(), makeAttachment("a1.csv")));
    msgs.add(new EmailMessage("subj2", "no@ivers.ru",
        new Date(), makeAttachment("a2.csv")));
    return msgs;
  }


  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG;
    rule.id = "id";
    rule.header = "Subject";
    rule.contains = "\"Москва";
    rule.filerules = new ArrayList<AttachmentTaggingRule>(){{
      add(new AttachmentTaggingRule("1", TAG1));
      add(new AttachmentTaggingRule("2", TAG2));
    }};
    return Collections.singletonList(rule);
  }
}
