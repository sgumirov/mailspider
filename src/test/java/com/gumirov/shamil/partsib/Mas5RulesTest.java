package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MAS-5
 */
public class Mas5RulesTest extends AbstractMailAutomationTest {

  @Test
  public void test() throws Exception {
    launch("acceptedmail", "taglogger",
        Collections.singletonList("2097.0.mainavtomir"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)),
        new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("MAS-5/central_warehouse_toyota_nsk.eml"))
    );
  }

  @Override
  public List<EmailAcceptRule> getAcceptRules() {
    try {
      return new JsonParser<EmailAcceptRule>().loadList("MAS-5/email_accept_rules.json", EmailAcceptRule.class, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Cannot read accept rules", e);
    }
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("MAS-5/tag_rules.json");
  }
}
