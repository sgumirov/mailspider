package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.ArrayList;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class GreenMailUtils {
  public String getHost(){
    return "127.0.0.1";
  }
  public GreenMailRule getGreenMailRules(int count){
    ServerSetup[] configs = new ServerSetup[count];
    configs[0] = new ServerSetup(1143, getHost(), ServerSetup.PROTOCOL_IMAP);
    configs[1] = new ServerSetup(1993, getHost(), ServerSetup.PROTOCOL_IMAPS);
    return new GreenMailRule(configs);
  }

  public ArrayList<Endpoint> getGreenmailEndpoints(int count) {
    ArrayList<Endpoint> e = new ArrayList<>();
    e.add(createGreenmailEndpoint("imap", 1143)); //greenmail 1
    e.add(createGreenmailEndpoint("imaps", 1993)); //greenmail 2
    return e;
  }

  private Endpoint createGreenmailEndpoint(String protocol, int port) {
    Endpoint e = new Endpoint();
    e.id = "Greenmail-" + protocol + "-" + port;
    e.url = protocol + "://"+getHost()+":" + port;
    e.delay = "5000";
    e.user = getLogin();
    e.pwd = getPassword();
    return e;
  }

  private String getPassword() {
    return "passwd";
  }

  public String getLogin() {
    return "login";
  }
}
