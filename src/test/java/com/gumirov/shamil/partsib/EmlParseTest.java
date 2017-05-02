package com.gumirov.shamil.partsib;

import org.apache.commons.lang3.StringUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class EmlParseTest {
  public static void main(String[] args) throws IOException, MessagingException {
    Properties props = System.getProperties();
    props.put("mail.host", "smtp.dummydomain.com");
    props.put("mail.transport.protocol", "smtp");

    Session mailSession = Session.getDefaultInstance(props, null);
    InputStream source = new FileInputStream("src/test/resources/message.eml");
    MimeMessage msg = new MimeMessage(mailSession, source);
    parse(msg, 0);
  }

  static void parse(MimeMultipart mpart, int level) throws MessagingException, IOException {
    log(level, "count="+mpart.getCount());
    for (int i = 0; i < mpart.getCount(); ++i){
      BodyPart part = mpart.getBodyPart(i);
      String fname = part.getFileName(); fname = fname == null ? "" : fname;
      String ctype = part.getContentType();
      log(level, "bodypart ["+ctype+"] filename="+fname+" decoded="+MimeUtility.decodeText(fname)+" size="+part.getSize());
      parseContent(part.getContent(), level);
    }
  }
  private static void parse(MimeMessage msg, int level) throws IOException, MessagingException {
    String fname = msg.getFileName(); fname = fname == null ? "" : fname;
    String ctype = msg.getContentType();
    log(level, "bodypart ["+ctype+"] filename="+fname+" decoded="+MimeUtility.decodeText(fname)+" size="+msg.getSize());
    parseContent(msg.getContent(), level);
  }
  static void parseContent(Object msg, int level) throws IOException, MessagingException {
    if (msg instanceof MimeMultipart) {
      parse((MimeMultipart) msg, level+1);
      return;
    } else if (msg instanceof MimeMessage) {
      parse((MimeMessage) msg, level+1);
      return;
    } else {
//      log(level, "LEAF. Content=" + msg);
      return;
    }
  }

  static void log(int level, String s){
    System.out.println(StringUtils.repeat("--", level)+s);
  }
}
