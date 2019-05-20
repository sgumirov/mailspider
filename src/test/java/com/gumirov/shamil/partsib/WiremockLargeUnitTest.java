package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.junit.Ignore;
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
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
@Ignore
public class WiremockLargeUnitTest {
  private static final String INSTANCE_ID = "1";
  private static final String SOURCE_ID = "2";
  final int port = 18088;
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(port));
  private Logger log = LoggerFactory.getLogger(WiremockLargeUnitTest.class.getSimpleName());
  private final String endpoint = "/endpoint";

  @Test
  public void testWiremock() throws IOException {
    //http mock endpoint setup
    stubFor(post(urlEqualTo(endpoint))
        .willReturn(aResponse()
            .withStatus(200)));

    byte[] b = new byte[22000000]; //22M
    HttpPostFileSender sender = new HttpPostFileSender("http://127.0.0.1:"+port+endpoint);
    sender.onOutput("123.csv", "123", b, b.length, 1000000, "mailid", INSTANCE_ID, SOURCE_ID);

    //verify http mock endpoint
    WireMock.verify(
        22,
        WireMock.postRequestedFor(urlPathEqualTo(endpoint))
            .withHeader("X-Instance-Id", equalTo(INSTANCE_ID))
            .withHeader("X-Source-Endpoint-Id", equalTo(SOURCE_ID))
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
