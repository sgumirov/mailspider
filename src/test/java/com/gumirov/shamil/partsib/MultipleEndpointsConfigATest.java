package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import org.junit.Ignore;

import java.util.*;

/**
 * Check startup exception for multiple source email endpoints.
 *
 * @author shamil@gumirov.com
 * Copyright (c) 2019 by Shamil Gumirov.
 */
@Ignore
public class MultipleEndpointsConfigATest extends DeleteOldMailATest {

  @Override
  public ArrayList<Endpoint> getEmailEndpoints() {
    return new ArrayList<Endpoint>(){{
      Endpoint email = new Endpoint();
      email.id = "delete_mail_test_endpoint_01";
      email.url = imapUrl;
      email.user = login;
      email.pwd = pwd;
      email.parameters = new HashMap<>();
      email.delay = "1000";
      add(email);
      email = new Endpoint();
      email.id = "delete_mail_test_endpoint_02";
      email.url = imapUrl.replace("127.0.0.1", "localhost");
      email.user = login;
      email.pwd = pwd;
      email.parameters = new HashMap<>();
      email.delay = "1500";
      add(email);
    }};
  }

}
