package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.junit.Rule;
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
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class WiremockLargeTest {
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(8080));
  private Logger log = LoggerFactory.getLogger(WiremockLargeTest.class.getSimpleName());
  private final String endpoint = "/endpoint";

  @Test
  public void testWiremock() throws IOException {
    //http mock endpoint setup
    stubFor(post(urlEqualTo(endpoint))
        .willReturn(aResponse()
            .withStatus(200)));

    byte[] b = new byte[22000000]; //22M
    HttpPostFileSender sender = new HttpPostFileSender("http://127.0.0.1:8080"+endpoint);
    sender.onOutput("123.csv", "123", b, b.length, 1000000, "mailid");

    //verify http mock endpoint
    WireMock.verify(
        22,
        WireMock.postRequestedFor(urlPathEqualTo(endpoint))
    );
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
}
