package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;

import java.util.*;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class SpecialCharsFilteringAutomatedTest extends AbstractMailAutomationTest {
  private static final String TAG = "TAG";
  private static final String TAG1 = "TAG1";
  private static final String TAG2 = "TAG2";

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
    for (EmailMessage m : msgs) sendTo.put(m, ENDP1);
    launch(names, getExpectTags(names), names.size() , sendTo);
  }

  private List<String> getExpectTags(List<String> filenames) {
    ArrayList<String> tags = new ArrayList<>();
    tags.add(TAG1);
    tags.add(TAG2);
    tags.add(TAG);
    return tags;
  }

  private List<EmailMessage> createMessages() {
    ArrayList<EmailMessage> msgs = new ArrayList<>();
    msgs.add(new EmailMessage("Москва.. \"блабла\"", "no@ivers.ru",
        new Date(), makeAttachment("a1.csv")));
    msgs.add(new EmailMessage("Москва.. jkfdshfkjdsh () Москва.. (авиа) skjfdndsf", "no@ivers.ru",
        new Date(), makeAttachment("a2.csv")));
    msgs.add(new EmailMessage("jkfdshfkjdsh () Москва (новосиб) skjfdndsf", "no@ivers.ru",
        new Date(), makeAttachment("a3.csv")));
    return msgs;
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    ArrayList<PricehookIdTaggingRule> list = new ArrayList<>();
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG1;
    rule.id = "id";
    rule.header = "Subject";
    rule.contains = "Москва.. \"";
    list.add(rule);
    rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG2;
    rule.id = "id";
    rule.header = "Subject";
    rule.contains = "Москва.. (авиа)";
    rule.filerules = new ArrayList<AttachmentTaggingRule>(){{
      add(new AttachmentTaggingRule("1", TAG1));
      add(new AttachmentTaggingRule("2", TAG2));
    }};
    list.add(rule);
    rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG;
    rule.id = "id";
    rule.header = "Subject";
    rule.contains = "(новосиб)";
    rule.filerules = new ArrayList<AttachmentTaggingRule>(){{
      add(new AttachmentTaggingRule("1", TAG1));
      add(new AttachmentTaggingRule("2", TAG2));
    }};
    list.add(rule);
    return list;
  }
}
