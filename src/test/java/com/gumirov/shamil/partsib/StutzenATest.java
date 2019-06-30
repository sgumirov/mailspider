package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.RawEmailMessage;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Issue 2061: Stutzen email not accepted.
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class StutzenATest extends AbstractMailAutomationTest {

  final String username = "partsibprice@mail.ru";
  private final String tagExpect = "2023.0.main11";
  private final String attachName = "novosib.csv";
  final String expectedSubj = "Прайс-лист от www.stutzen.ru Склад Новосибирск";
  final String emailRaw = "stutzen/stutzen-nsk.txt";
  final protected String to = "partsibprice@mail.ru", login = to, pwd = "password";

  @Rule
  public GreenMailRule greenMail =  new GreenMailRule(ServerSetupTest.IMAP);

  @Before
  public void cleanGreenMail() throws IOException, MessagingException {
    greenMail.reset();
//    GreenMailUser user = greenMail.setUser(username, username, pwd);
//    user.deliver(GreenMailUtil.newMimeMessage(getClass().getClassLoader().getResourceAsStream(emailRaw)));
  }

  @Test
  public void test() throws Exception {
//    1. create mimemessage from is
//    2. send it, check tags
    launch("acceptedmail", "taglogger",
        new ArrayList<String>(){{add(tagExpect);}},
        new ArrayList<String>(){{add(attachName);}},
        1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("stutzen/stutzen-nsk.txt"))
    );
  }

  @Override
  protected void removeSourceEndpoints(String endpointId) throws Exception {
    //none to remove
  }

  @Override
  public Map<String, String> getConfig() {
    HashMap<String, String> map = new HashMap<>();
    map.put("email.enabled", "true");
    map.put("tracing", "false");
    return map;
  }

  @Override
  public ArrayList<Endpoint> getEmailEndpoints() {
    Endpoint email = new Endpoint();
    email.id = "stutzen_test_endpoint";
    email.url = "imap://partsib@127.0.0.1:3143";
    email.user = username;
    email.pwd = pwd;
    email.parameters = new HashMap<>();
    email.delay = "1000";
    return new ArrayList<Endpoint>(){{
      add(email);
    }};
  }

  @Override
  public ArrayList<EmailAcceptRule> getAcceptRules() {
    ArrayList<EmailAcceptRule> rules = new ArrayList<>();
    EmailAcceptRule r = new EmailAcceptRule();
    r.header="From";
    r.contains="@";
    rules.add(r);
    return rules;
  }

  @Override
  public long getMillisecondsWaitBeforeAssert() {
    return 30000;
  }

  @Override
  public void sendMessagesToEndpoints(Map<EmailMessage, String> toSend) {
    GreenMailUser user = greenMail.setUser(username, username, pwd);
    user.deliver(GreenMailUtil.newMimeMessage(getClass().getClassLoader().getResourceAsStream(emailRaw)));
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("stutzen/prod_rules_01_08_18.json");
  }
}
