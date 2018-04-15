package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.AllMailRetriever;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.UnseenRetriever;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Flags;
import javax.mail.Message;
import java.util.*;

/**
 * Automated test for deletion of old messages.
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class DeleteOldMailATest extends AbstractMailAutomationTest {
  @Rule
  public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP);

  final private int imapport = 3143;
  final private String imapUrl = "imap://127.0.0.1"+":"+imapport;
  final private String to = "partsibprice@mail.ru", login = to, pwd = "password";

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Collections.nCopies(1, "944.0.main"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new EmailMessage("subj1", "no@ivers.ru",
            new Date(System.currentTimeMillis() - MainRouteBuilder.DAY_MILLIS),
            makeAttachment("a1.csv")),
        new EmailMessage("subj2", "no@ivers.ru",
            new Date(System.currentTimeMillis() - 7 * MainRouteBuilder.DAY_MILLIS),
            makeAttachment("a2.csv")),
        new EmailMessage("no1", "no1@gmail.com",
            new Date(System.currentTimeMillis() - MainRouteBuilder.DAY_MILLIS),
            makeAttachment("a3.csv")),
        new EmailMessage("no2", "no2@gmail.com",
            new Date(System.currentTimeMillis() - 7 * MainRouteBuilder.DAY_MILLIS),
            makeAttachment("a4.csv"))
    );
  }

  @Override
  public ArrayList<EmailAcceptRule> getAcceptRules() {
    ArrayList<EmailAcceptRule> rules = new ArrayList<>();
    EmailAcceptRule r = new EmailAcceptRule();
    r.header="Subject";
    r.contains="hi";
    rules.add(r);
    return rules;
  }

  @Override
  public void waitBeforeAssert() {
    try {
      Thread.sleep(30000);
      log.info("Sleeped well. Waking up.");
    } catch (InterruptedException e) {
      log.error("Cannot sleep. Insomnia?", e);
    }
  }

  @Override
  public void assertConditions() throws Exception {
    Message[] messages;

    UnseenRetriever unseenRetriever = new UnseenRetriever(greenMail.getImap());
    messages = unseenRetriever.getMessages(login, pwd);
    log.info("Number of Unseen messages left in mailbox: "+messages.length);
    assertEquals(2, messages.length);
    unseenRetriever.close();

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

    assertTrue("Must delete some mail", super.builder.getDeletedMailCount() > 0);
  }

  @Override
  public Map<String, String> getConfig() {
    HashMap<String, String> map = new HashMap<>();
    map.put("delete_old_mail.enabled", "true");
    map.put("delete_old_mail.keep.days", "5");
    map.put("delete_old_mail.check_period.hours", "0");
    map.put("email.enabled", "true");
    map.put("tracing", "false");
    return map;
  }

  @Override
  public ArrayList<Endpoint> getEmailEndpoints() {
    Endpoint email = new Endpoint();
    email.id = "delete_mail_test_endpoint";
    email.url = imapUrl;
    email.user = login;
    email.pwd = pwd;
    email.parameters = new HashMap<>();
    //email.parameters.put("delete", "true");
    email.delay = "1000";
    return new ArrayList<Endpoint>(){{
      add(email);
    }};
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("prod_rules.json");
  }

  //send mail via greenmail
  @Override
  public void sendMessagesToEndpoints(Map<EmailMessage, String> toSend) {
    GreenMailUser user = greenMail.setUser(login, to, pwd);
    //we ignore endpoint name here as we use greenMail
    for (EmailMessage m : toSend.keySet()) {
      MailUtil.sendMessage(user, to, m, greenMail);
    }
  }
}
