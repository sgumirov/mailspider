package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoader;
import com.gumirov.shamil.partsib.util.Util;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class HeadersSendingRulesLoaderTest
    extends AbstractMailAutomationTest
{
  private final String INSTANCE_ID = "instance_at";
  private final String SOURCE_ID = "source.id";

  @Before
  public void before() throws IOException {
    stubFor(get(urlEqualTo("/load"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(loadTestResource("test_tag_rules.json"))
        ));
  }

  private byte[] loadTestResource(String fileName) throws IOException {
    return Util.readFully(
        AbstractMailAutomationTest.class.getClassLoader().getResourceAsStream(fileName));
  }

  @Override
  public Map<String, String> getConfig() {
    HashMap<String, String> config = new HashMap<>();
    config.put("instance.id", INSTANCE_ID);
    config.put("pricehook.config.url", "http://127.0.0.1:"+httpMock.port()+"/load");
    return config;
  }

  @Override
  protected String getDefaultEmailEndpointId() {
    return SOURCE_ID;
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.pricehookid = "tag1";
    rule.header = "From";
    rule.contains = "no@ivers.ru";
    rule.id = "tag1";
    return Collections.singletonList(rule);
  }

  @Override
  protected PricehookIdTaggingRulesConfigLoader createTestPricehookConfigLoader() {
    return (url, exchange) -> builder.loadPricehookConfig(url, exchange);
  }

  @Test
  public void test() throws Exception {
    EmailMessage email = new EmailMessage("test", "no@ivers.ru",
        makeAttachment("Прайс ру.csv"));
    launch(null, null, 1, Collections.singletonMap(email,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0))));
  }

  @Override
  public void assertPostConditions() {
    WireMock.verify(
        WireMock.getRequestedFor(urlPathEqualTo("/load"))
            .withHeader("X-Instance-Id", equalTo(INSTANCE_ID))
            .withHeader("X-Source-Endpoint-Id", equalTo(SOURCE_ID))
    );
  }
}
