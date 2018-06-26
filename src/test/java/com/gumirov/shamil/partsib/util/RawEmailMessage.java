package com.gumirov.shamil.partsib.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Email message for testing which can parse message saved in RFC 2822 with attachments.
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class RawEmailMessage extends EmailMessage {
  Logger log = LoggerFactory.getLogger(getClass());

  public RawEmailMessage(InputStream is) throws MessagingException, IOException {
    super(null);
    if (is == null)
      throw new IllegalArgumentException("RawEmailMessage arg InputStream must not be null");
    Session ses = Session.getDefaultInstance(new Properties());
    MimeMessage msg = new MimeMessage(ses, is);
    subject = msg.getSubject();
    attachments = new HashMap<>();
    from = msg.getFrom()[0].toString();
    String disposition = msg.getDisposition();
    if (disposition != null && disposition.contains("attachment")) {
      //Extract attachment filename from headers
      String filename = msg.getFileName();
      //We need not only main header value, but parameters. They are not parsed my MimeMessage, so we do it here manually
      String[] headers = {"Content-Type", "Content-type", "Content-Disposition", "Content-disposition"};
      String[] params = {"name", "filename"};
      for (int i = 0; filename == null && i < headers.length; ++i) {
        String[] headerValues = msg.getHeader(headers[i]);
        if (headerValues == null || headerValues.length == 0)
          continue;
        for (int z = 0; filename == null && z < headerValues.length; ++z) {
          for (int j = 0; filename == null && j < params.length; ++j) {
            if (headers[i].toLowerCase().contains("type")) {
              try {
                filename = new ContentType(headerValues[z]).getParameter(params[j]);
              } catch (ParseException e) {
                log.debug("cannot parse: header='" + headers[i] + "' val='" + headerValues[z] + "'", e);
                continue;
              }
            } else {
              try {
                filename = new ContentDisposition(headerValues[z]).getParameter(params[j]);
              } catch (ParseException e) {
                log.debug("cannot parse: header='" + headers[i] + "' val='" + headerValues[z] + "'", e);
                continue;
              }
            }
          }
        }
      }
      filename = MimeUtility.decodeText(filename);
      if (filename != null) {
        DataHandler dh = new DataHandler(msg.getContent(), msg.getContentType());
        attachments.put(filename, dh);
      }
    } else {
      handleMessage(msg);
    }
  }

  public void handleMessage(javax.mail.Message message) throws IOException, MessagingException {
    Object content = message.getContent();
    String contentType = message.getContentType();
    if (content instanceof String) {
      throw new RuntimeException("not yet impl");
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
        //test if attachment and contains filename:
        if (bp.getFileName() != null &&
            (bp.getDisposition() == null || bp.getDisposition().toLowerCase().contains(Part.ATTACHMENT))) {
          attachments.put(bp.getFileName(), new DataHandler(content, bp.getContentType()));
        } else //this is Body
          log.info("Body parsing not impl yet. TODO if needed add field 'body' to EmailMessage");
      } else if (content instanceof InputStream) {
        if (bp.getFileName() != null &&
            (bp.getDisposition() == null || bp.getDisposition().toLowerCase().contains(Part.ATTACHMENT))) {
          attachments.put(bp.getFileName(), new DataHandler(content, bp.getContentType()));
          log.info("Attachment found: "+bp.getFileName()+" with size="+((InputStream) content).available());
        } else //this is Body
          log.info("Body parsing not impl yet. TODO add field 'body' to EmailMessage");
      } else if (content instanceof javax.mail.Message) {
        handleMessage((javax.mail.Message) content);
      } else if (content instanceof Multipart) {
        handleMultipart((Multipart) content);
      } else {
        log.error("Cannot process message content: class=" + content.getClass());
        throw new RuntimeException("not yet impl");
      }
    }
  }
}
