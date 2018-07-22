package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.GreenMailUtils;
import com.icegreen.greenmail.junit.GreenMailRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class ParallelEmailProcessingTest extends AbstractMailAutomationTest {
  private static final int MAIL_ENDPOINTS_COUNT = 2;
  private static final String TAG = "TAG";
  private static final String TAG1 = "TAG1";
  private static final String TAG2 = "TAG2";
  protected GreenMailUtils utils = new GreenMailUtils();

  @Rule
  public final GreenMailRule greenMail = utils.getGreenMailRules(MAIL_ENDPOINTS_COUNT);

  @Test
  public void test() throws Exception {
    List<EmailMessage> msgs = createMessages(MAIL_ENDPOINTS_COUNT);
    List<String> names = new ArrayList<>();
    for (EmailMessage m : msgs)
      for (String name : m.attachments.keySet())
        names.add(name);
    ArrayList<Endpoint> endpoints = getEmailEndpoints();
    final String ENDP1 = EndpointSpecificUrl.apply("direct:emailreceived", endpoints.get(0));
    final String ENDP2 = EndpointSpecificUrl.apply("direct:emailreceived", endpoints.get(1));
    Map<EmailMessage, String> sendTo = new HashMap<>();
    sendTo.put(msgs.get(0), ENDP1);
    sendTo.put(msgs.get(1), ENDP1);
    sendTo.put(msgs.get(2), ENDP2);
    sendTo.put(msgs.get(3), ENDP2);
    launch(names, getExpectTags(names), names.size(), sendTo);
    //todo check how wiremock assertion for attachment names works in superclass
  }

  private List<String> getExpectTags(List<String> names) {
    ArrayList<String> tags = new ArrayList<>();
    for (String n : names) {
      if (n.contains("1")) tags.add(TAG1);
      if (n.contains("2")) tags.add(TAG2);
    }
    return tags;
  }

  private List<EmailMessage> createMessages(int mailEndpointsCount) {
    ArrayList<EmailMessage> msgs = new ArrayList<>();
    for (int i = 0; i < mailEndpointsCount; ++i) {
      msgs.add(new EmailMessage("subj1", "no@ivers.ru",
          new Date(), makeAttachment("a1.csv")));
      msgs.add(new EmailMessage("subj2", "no@ivers.ru",
          new Date(), makeAttachment("a2.csv")));
    }
    return msgs;
  }

  @Override
  public ArrayList<Endpoint> getEmailEndpoints() {
    return utils.getGreenmailEndpoints(MAIL_ENDPOINTS_COUNT);
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.pricehookid = TAG;
    rule.id = "id";
    rule.header = "From";
    rule.contains = "@";
    rule.filerules = new ArrayList<AttachmentTaggingRule>(){{
      add(new AttachmentTaggingRule("1", TAG1));
      add(new AttachmentTaggingRule("2", TAG2));
    }};
    return Collections.singletonList(rule);
  }
}

