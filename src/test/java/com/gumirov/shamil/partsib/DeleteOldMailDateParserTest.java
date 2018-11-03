package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.AllMailRetriever;
import com.gumirov.shamil.partsib.util.EmailMessage;

import javax.mail.Flags;
import javax.mail.Message;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class DeleteOldMailDateParserTest extends DeleteOldMailATest {
  //add test values here:
  public static String[] badDates = {"28 Mar 2018 05:23:32 +0300", "28 Mar 2018 05:23:32 +0300"};

  private static SimpleDateFormat badDateParser = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");
  public static Date[] correctDates = new Date[badDates.length];
  static {
    try {
      for (int i = 0; i < correctDates.length; ++i)
        correctDates[i] = badDateParser.parse(badDates[i]);
    } catch (ParseException e) {
      throw new RuntimeException("Cannot parse date", e);
    }
  }

  @Override
  protected EmailMessage[] getMessages() {
    EmailMessage[] msgs = super.getMessages();
    //todo generate messages with bad formatted date
    int i = 0;
    for (EmailMessage m : msgs) {
      if (i > 1) {
        //set now
        m.date = new Date();
        continue;
      }
      //set old and bad
      m.date = null;
      m.setHeader("Date", badDates[i++]);
    }
    return msgs;
  }

  @Override
  public void assertConditions() throws Exception {
    super.assertConditions();
    Message[] messages;
    AllMailRetriever allRetriever = new AllMailRetriever(greenMail.getImap());
    messages = allRetriever.getMessages(login, pwd);
    int notDeleted = 0;
    for (Message m : messages) {
      if (m.isSet(Flags.Flag.DELETED)) continue;
      else ++notDeleted;
    }
    log.info("Total number of messages left in mailbox: "+notDeleted);
    assertEquals(2, notDeleted);
    allRetriever.close();

    assertTrue("Must delete some mail", super.builder.getDeletedMailCount() == 2);
  }
}
