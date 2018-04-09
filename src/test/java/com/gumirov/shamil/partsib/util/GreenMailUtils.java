package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.ArrayList;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class GreenMailUtils {
  public String getHost(){
    return "127.0.0.1";
  }
  public GreenMailRule getGreenMailRules(int count){
    ServerSetup[] configs = new ServerSetup[count];
    for (int i = 0; i < configs.length; ++i) {
      configs[i] = new ServerSetup(ServerSetup.PORT_IMAP + i,getHost(), ServerSetup.PROTOCOL_IMAP);
    }
    return new GreenMailRule(configs);
  }

  public ArrayList<Endpoint> getGreenmailEndpoints(int count) {
    ArrayList<Endpoint> e = new ArrayList<>();
    e.add(createGreenmailEndpoint(ServerSetup.PORT_IMAP)); //greenmail 1
    e.add(createGreenmailEndpoint(ServerSetup.PORT_IMAP + 1)); //greenmail 2
    return e;
  }

  private Endpoint createGreenmailEndpoint(int port) {
    Endpoint e = new Endpoint();
    e.id = "Greenmail-imap-"+port;
    e.url = "imap://"+getHost()+":"+port;
    e.delay = ""+(5000+1000*port-ServerSetup.PORT_IMAP);
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
