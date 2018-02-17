package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.Util;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.gumirov.shamil.partsib.MainRouteBuilder.DAY_MILLIS;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class MailUtil {
  public static void sendMessage(GreenMailUser user, String to, AbstractMailAutomationTest.EmailMessage msg, GreenMailRule greenMail) {
    HashMap<String, byte[]> attach = new HashMap<>();
    try{
      for (String fn : msg.attachments.keySet()) {
        attach.put(MimeUtility.encodeText(fn), Util.readFully(msg.attachments.get(fn).getInputStream()));
      }
      user.deliver(createMimeMessage(to, msg.from, msg.subject, msg.date, attach, greenMail));
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public static MimeMessage createMimeMessage(String to, String from, String subject, Date date, Map<String, byte[]> attachments, GreenMailRule greenMail)
      throws MessagingException {
    MimeMessage msg = GreenMailUtil.createTextEmail(to, from, subject, "body", greenMail.getImap().getServerSetup());
    msg.setSentDate(date);
    Multipart multipart = new MimeMultipart();
    for (String fname : attachments.keySet()) {
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      byte[] file = attachments.get(fname);
      messageBodyPart.setDataHandler(new DataHandler(file, "application/vnd.octet-stream"));
      messageBodyPart.setFileName(fname);
      multipart.addBodyPart(messageBodyPart);
    }
    msg.setContent(multipart);
    return msg;
  }
}

