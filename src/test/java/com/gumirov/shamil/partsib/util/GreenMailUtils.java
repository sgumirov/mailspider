package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.ArrayList;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class GreenMailUtils {

  private static final int IMAP_BASE_PORT = 1143;
  private static final int IMAPS_BASE_PORT = 1993;

  public String getHost(){
    return "127.0.0.1";
  }

  public GreenMailRule getGreenMailRules(int count){
    ServerSetup[] configs = new ServerSetup[count];
    configs[0] = new ServerSetup(IMAP_BASE_PORT, getHost(), ServerSetup.PROTOCOL_IMAP);
    configs[1] = new ServerSetup(IMAPS_BASE_PORT, getHost(), ServerSetup.PROTOCOL_IMAPS);
    return new GreenMailRule(configs);
  }

  public ArrayList<Endpoint> getGreenmailEndpoints(int count) {
    //todo create any number of endpoints
    ArrayList<Endpoint> e = new ArrayList<>();
    e.add(createGreenmailEndpoint("imap", IMAP_BASE_PORT)); //greenmail 1
    e.add(createGreenmailEndpoint("imaps", IMAPS_BASE_PORT)); //greenmail 2
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
