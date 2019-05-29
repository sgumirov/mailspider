package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.RawEmailMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Stub test to check startup exception on load specific config.
 *
 * @author shamil@gumirov.com
 * Copyright (c) 2019 by Shamil Gumirov.
 */
@Ignore
public class EndpointsConfigATest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Arrays.asList("11969.0.main"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("kumir54.eml"))
    );
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagRules("prod_rules.json");
  }

  @Override
  protected Endpoints loadTestEndpointsConfig() {
    try {
      String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("multiple_endpoints/multiple_endpoints.json"), Charset.defaultCharset());
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(json, Endpoints.class);
    } catch (IOException e) {
      assertFalse("Must load json file without exceptions", true);
      return null;
    }
  }
}
