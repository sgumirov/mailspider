package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class WiremockLargeAutomationTest extends AbstractMailAutomationTest {
  private Logger log = LoggerFactory.getLogger(WiremockLargeAutomationTest.class.getSimpleName());
  private final String endpoint = "/endpoint";

  @Test
  public void testWiremock() throws IOException {
    byte[] b = new byte[22000000]; //22M
//    byte[] b = "1234567890123456".getBytes(); //22M
    HttpPostFileSender sender = new HttpPostFileSender("http://127.0.0.1:"+ getHttpMockPort()+endpoint);
//    sender.onOutput("123.csv", "123", b, b.length, 4);
    sender.onOutput("123.csv", "123", b, b.length, 1000000, "mid");

/*
    //verify http mock endpoint
    WireMock.verify(
        22,
        WireMock.postRequestedFor(urlPathEqualTo(endpoint))
    );
*/
    //verify attachments' names
    Map<String, InputStream> atts = new HashMap<>();
    Map<String, String> tags = new HashMap<>();
    List<LoggedRequest> reqs = WireMock.findAll(postRequestedFor(urlPathEqualTo(endpoint)));
    for (LoggedRequest req : reqs) {
      byte[] fb = Base64.getDecoder().decode(req.getHeader("X-Filename"));
      String fname = new String(fb, "UTf-8");
      String tag = req.getHeader("X-Pricehook");
      atts.put(fname, new ByteArrayInputStream(req.getBody()));
      tags.put(fname, tag);
    }
    //print tags
    log.info("HTTP mock POSTed file names with tags:");
    for (String fname : tags.keySet()) {
      log.info(fname + " : " + tags.get(fname));
    }
    log.info("HTTP mock POSTed file:");
    for (String fname : atts.keySet()) {
      log.info(fname + " : " + atts.get(fname));
    }
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagRules("prod_rules.json");
  }
}
