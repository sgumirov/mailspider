package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;

import javax.activation.DataHandler;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * NullPointerException in log is expected for this test.
 *
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NotificationsATest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    Map<String, DataHandler> attachments = Collections.singletonMap("a.csv",
//        new DataHandler("hi there", "text/plain")
        new DataHandler(new ByteArrayInputStream("hi there".getBytes()), "text/plain")
    );

    //send 2 messages, expect 1 notification
    launch("acceptedmail", "taglogger",
        Arrays.asList("982.0.lamps", "982.0.lamps"),
        null, 2,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new EmailMessage("Price", "office@dinamikasveta.ru", attachments),
        new EmailMessage("Price", "office@dinamikasveta.ru", attachments)
    );
  }

  @Override
  public int getExpectedNotificationCount() {
    return 1;
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagRules("tagrules.json");
  }
}
