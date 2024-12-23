package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.Util;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;

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

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class MailUtil {
  public static void sendMessage(GreenMailUser user, String to, EmailMessage msg, GreenMailRule greenMail) {
    HashMap<String, byte[]> attach = new HashMap<>();
    try{
      for (String filename : msg.attachments.keySet()) {
        attach.put(MimeUtility.encodeText(filename), Util.readFully(msg.attachments.get(filename).getInputStream()));
      }
      user.deliver(overrideHeaders(msg, createMimeMessage(to, msg.from, msg.subject, msg.date, attach, greenMail)));
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public static MimeMessage createMimeMessage(String to, String from, String subject, Date date,
                                              Map<String, byte[]> attachments, GreenMailRule greenMail)
    throws MessagingException
  {
    MimeMessage msg = GreenMailUtil.createTextEmail(to, from, subject, "body", greenMail.getImap().getServerSetup());
    if (date != null) {
      msg.setSentDate(date); //don't remove if set explicitly via setHeader
    }
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

  public static MimeMessage overrideHeaders(EmailMessage msg, MimeMessage mime)
    throws MessagingException
  {
    for (String k : msg.getHeaders().keySet()) {
      mime.setHeader(k, msg.getHeader(k));
    }
    return mime;
  }
}

