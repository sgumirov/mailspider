package com.gumirov.shamil.partsib;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class MockMailTest {
  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

  @Test
  public void testReceive() throws MessagingException {
    GreenMailUser user = greenMail.setUser("to@localhost.com", "login-id", "password");
    user.deliver(createMimeMessage()); // You can either create a more complex message...
    GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com",
        "subject", "body"); // ...or use the default messages

    assertEquals(2, greenMail.getReceivedMessages().length); // // --- Place your POP3 or IMAP retrieve code here
  }

  private MimeMessage createMimeMessage(String to, String from, String subject, Map<String, byte[]> attachments) throws MessagingException {
    MimeMessage msg = GreenMailUtil.createTextEmail(to, from, subject, "body", greenMail.getImap().getServerSetup());
    Multipart multipart = new MimeMultipart();
    for (String fname : attachments.keySet()) {
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      byte[] file = attachments.get(fname);
      String fileName = fname;
      messageBodyPart.setDataHandler(new DataHandler(file, "application/octet-stream"));
      messageBodyPart.setFileName(fname);
      multipart.addBodyPart(messageBodyPart);
    }
    msg.setContent(multipart);
    return msg;
  }
}
