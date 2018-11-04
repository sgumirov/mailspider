package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.AllMailRetriever;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.SkipMessageException;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.Flags;
import javax.mail.Message;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class ErrorHandlerTest extends CamelTestSupport {

  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP);

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;

  final private int imapport = 3143;
  final private String imapUrl = "imap://127.0.0.1"+":"+imapport;
  final private String to = "partsibprice@mail.ru", login = to, pwd = "password";
  private static long consumed;

  @Test
  public void testErrorHandling() throws InterruptedException {
    GreenMailUser user = greenMail.setUser(to, to, pwd);
    MailUtil.sendMessage(user, to, new EmailMessage("subj",
        to, makeAttachment("a.csv")), greenMail);
    MailUtil.sendMessage(user, to, new EmailMessage("Get Gmail for your mobile device",
        to, makeAttachment("a.csv")), greenMail);

    mockEndpoint.expectedMessageCount(1);
    mockEndpoint.assertIsSatisfied();
  }

  public Map<String, DataHandler> makeAttachment(String name) {
    InputStream is = new ByteArrayInputStream("Hello Email World, yeah!".getBytes());
    return Collections.singletonMap(name, new DataHandler(is, "text/plain"));
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        String url = format(
            "%s?password=%s&username=%s" +
                "&consumer.delay=%s" +
//                "&consumer.useFixedDelay=true" +
                "&consumer.initialDelay=1" +
                "&delete=true" +
//                "&searchTerm.toSentDate=now-%sh" +
                "&searchTerm.unseen=false"+ //older than daysStore
                "&unseen=false" +
                "&fetchSize=250" +
                "&skipFailedMessage=true" +
                "&maxMessagesPerPoll=250"+
                "&mail.imap.ignorebodystructuresize=true"+
                "&mail.imap.partialfetch=false"+
                "&mail.imaps.partialfetch=false"+
                "&mail.debug=true"/*+
                "%s"*/,
            imapUrl, URLEncoder.encode(pwd, "UTF-8"), URLEncoder.encode(login, "UTF-8"),
            1500, 30*24 /*hours, Util.formatParameters(email.parameters)*/);

        onException(SkipMessageException.class).process(new MyErrorHandler()).id("myerrorhandler").
            handled(true).
            stop();
        from(url).
            process(new MyProcessor()).id("myprocessor").
            log(LoggingLevel.INFO, "[$simple{in.header.Subject}]").
            to("mock:result");
      }
    };
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    super.context.setTracing(false);
  }

  @Override
  public void tearDown() throws Exception {
    AllMailRetriever allRetriever = new AllMailRetriever(greenMail.getImap());
    Message[] messages = allRetriever.getMessages(login, pwd);
    int notDeleted = 0;
    for (Message m : messages) {
      log.info(
          " inb inbox: subject="+m.getHeader("Subject")[0]+
          " id="+m.getHeader("Message-ID")[0]);
      if (m.isSet(Flags.Flag.DELETED)) continue;
      else ++notDeleted;
    }
    log.info("Total number of messages left in mailbox: "+notDeleted);
    assertEquals(1, notDeleted);
    allRetriever.close();

    super.tearDown();
  }

  private static class MyErrorHandler implements Processor {
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(Exchange exchange) {
      Exception e = exchange.getException();
      log.info("Skipped email with id="+exchange.getIn().getHeader("Message-ID")+" subj="+exchange.getIn().getHeader("Subject").toString()+" t="+e);
      exchange.getIn().setFault(true);
      exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
    }
  }

  private static class MyProcessor implements Processor {
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
      String subj = exchange.getIn().getHeader("Subject")+"";
      if (exchange.getIn().getHeader("Subject").equals("Get Gmail for your mobile device")) {
        log.info("consume mail: subj=" + subj);
        consumed++;
      } else {
        log.info("Skipping this: subj="+subj);
        throw new SkipMessageException("Skipping this: subj="+subj);
      }
    }
  }
}
