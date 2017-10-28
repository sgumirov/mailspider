package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class Kumir54Test extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Arrays.asList("10.1.123"),
        null, 1, "direct:emailreceived",
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("kumir54.eml"))
    );
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("partsib_tags_config2.json");
  }
}
