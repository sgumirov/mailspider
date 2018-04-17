package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 5/6/2017 Time: 21:15<br/>
 */
@Ignore
public class DoubleQuotesRawLetterParseUnitTest extends PricehookTagFilterUnitTest {
  @Test
  public void test() throws Exception {
    super.launch("acceptedmail", "taglogger",
      Arrays.asList("master"),
      null, 1,
      EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().email.get(0)), //send through first endpoint
      new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("double_quotes_bad.eml"))
    );
  }

  public class RawEmailMessage extends EmailMessage {
    public RawEmailMessage(InputStream is) throws MessagingException, IOException {
      super(null);
      Session ses = Session.getDefaultInstance(new Properties());
      MimeMessage msg = new MimeMessage(ses, is);
      this.subject = msg.getSubject();
      this.attachments = new HashMap<>();
      handleMessage(msg);
    }

    public void handleMessage(Message message) throws IOException, MessagingException {
      Object content = message.getContent();
      if (content instanceof String) {
//        attachments.put(bp.getFileName(), new DataHandler(content, "text/plain"));
      } else if (content instanceof Multipart) {
        Multipart mp = (Multipart) content;
        handleMultipart(mp);
      } else {
        throw new RuntimeException("not yet impl");
      }
    }

    public void handleMultipart(Multipart mp) throws MessagingException, IOException {
      int count = mp.getCount();
      for (int i = 0; i < count; i++) {
        BodyPart bp = mp.getBodyPart(i);
        Object content = bp.getContent();
        if (content instanceof String) {
          attachments.put(bp.getFileName(), new DataHandler(content, bp.getContentType()));
        } else if (content instanceof InputStream) {
          attachments.put(bp.getFileName(), new DataHandler(Util.readFully((InputStream) content),
              bp.getContentType()));
        } else if (content instanceof Message) {
          handleMessage((Message) content);
        } else if (content instanceof Multipart) {
          handleMultipart((Multipart) content);
        } else {
          log.error("Cannot process message content: class=" + content.getClass());
          throw new RuntimeException("not yet impl");
        }
      }
    }
  }

  public List<PricehookIdTaggingRule> getTagRules(){
    PricehookIdTaggingRule r3 = new PricehookIdTaggingRule();
    r3.header = "Subject";
    r3.contains = "Прайс-лист ООО \"Мастер Сервис\"  наличие";
    r3.pricehookid = "master";
    return Arrays.asList(r3);
  }
}
