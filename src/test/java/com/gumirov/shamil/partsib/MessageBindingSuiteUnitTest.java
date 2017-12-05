package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.component.direct.DirectEndpoint;
import org.junit.Test;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class MessageBindingSuiteUnitTest extends AbstractMailAutomationTest {

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("test_tag_rules.json");
  }

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Arrays.asList("977.0.msk.2"),
        null, 1, "direct:emailreceived",
        createMessages()
    );
  }

  private EmailMessage[] createMessages() {
    //todo implement test suite here and test success criteria
    return new EmailMessage[]{
        //new
        //todo
    };
  }

  //todo set endpoint

  public DirectEndpoint getSourceEndpoint() {
    DirectEndpoint e = new DirectEndpoint();
    e.setEndpointUriIfNotSpecified("direct:source");
    return e;
  }
}

class MessageTestUtil {
  static void setDisposition(String disposition, Message m) throws MessagingException {
    m.setDisposition(disposition);
  }

  static void setMultipart(boolean isMultipart, int num, Message m) throws MessagingException {
    if (isMultipart)
      m.setContent(createMultipart(num));
  }

  private static Multipart createMultipart(int num) throws MessagingException {
    BodyPart[] b = new BodyPart[num];
    for (int i = 0; i < b.length; i++) {
      InternetHeaders h = new InternetHeaders();
      b[i] = new MimeBodyPart(h, getPartContent(i));
    }
    Multipart m = new MimeMultipart();
    //m.
    //todo
    return m;
  }

  private static byte[] getPartContent(int i) {
    return null;
  }

  static void setTextBody(String textBody, Message m) throws MessagingException {
    m.setContent(textBody, "text/plain");
  }

  static void setAttachments(Map<String, byte[]> attachments, boolean asParts, Message m) {
    if (!asParts && attachments.size() > 1)
      throw new IllegalArgumentException("Cannot set multiple attachments as not multipart");
  }
}
