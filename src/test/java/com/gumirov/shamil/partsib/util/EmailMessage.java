package com.gumirov.shamil.partsib.util;

import javax.activation.DataHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class EmailMessage {
  public String subject;
  public Map<String, DataHandler> attachments;
  public String from;
  public Date date;
  protected HashMap<String, String> headers = new HashMap<>();

  public EmailMessage(String subject, String from, Date date, Map<String, DataHandler> attachments) {
    this(subject, from, attachments);
    this.date = date;
  }

  public EmailMessage(String subject, String from, Map<String, DataHandler> attachments) {
    this.subject = subject;
    this.from = from;
    this.attachments = attachments;
  }

  public EmailMessage(String subject, List<String> attachmentNames) {
    this.subject = subject;
    this.attachments = new HashMap<>();
    InputStream is = new ByteArrayInputStream(new byte[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'});
    for (String fname : attachmentNames) {
      attachments.put(fname, new DataHandler(is, "text/plain"));
    }
  }

  public EmailMessage(String subject) {
    this.subject = subject;
  }

  /**
   * Sets header value, overriding previous value.
   * @param k header name
   * @param v value
   * @return previous set value if any or null if none
   */
  public String setHeader(String k, String v) {
    return headers.put(k, v);
  }

  public String getHeader(String k) {
    return headers.get(k);
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
