package com.gumirov.shamil.partsib.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NotificationProcessor implements Processor {
  public static final String SKIP_NOTIFICATION = "skip-notification";
  private Properties config;
  protected Logger log = LoggerFactory.getLogger(this.getClass());
  private long lastSentNotificationTime;
  private final long NOTIFICATION_SEND_PERIOD;

  public NotificationProcessor(Properties config) {
    this.config = config;
    NOTIFICATION_SEND_PERIOD = Long.parseLong(config.getProperty("notification.period", "300000")); //5 min
  }

  @Override
  public void process(Exchange exchange) {
    if (lastSentNotificationTime == 0) {
      lastSentNotificationTime = System.currentTimeMillis();
    }
    else if (System.currentTimeMillis() - lastSentNotificationTime < NOTIFICATION_SEND_PERIOD) {
      exchange.getIn().setHeader(NotificationProcessor.SKIP_NOTIFICATION, true);
      return;
    }
    exchange.getOut().setHeader("from", config.getProperty("from", "partsibprice@yahoo.com"));
    exchange.getOut().setHeader("to", config.getProperty("to", "partsibprice@yahoo.com"));
    exchange.getOut().setHeader("subject", "MailSpider Notification");
    exchange.getOut().setHeader("contentType", "text/plain;charset=UTF-8");
    exchange.getOut().setBody("Notification");
    exchange.getOut().setAttachmentObjects(new HashMap<>()); //empty attachments
  }
}
