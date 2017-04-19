package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigProvider;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

import java.util.List;

/**
 *
 */
public class PricehookIdTaggingRulesLoaderProcessor implements Processor {

  private String url;
  private PricehookIdTaggingRulesConfigProvider configProvider;

  public PricehookIdTaggingRulesLoaderProcessor(String url, PricehookIdTaggingRulesConfigProvider configProvider) {
    this.url = url;
    this.configProvider = configProvider;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (url != null && configProvider != null) {
      List<PricehookIdTaggingRule> rules = configProvider.loadPricehookConfig(url);
      if (rules != null) {
        for (PricehookIdTaggingRule rule : rules) {
          rule.predicate = SimpleBuilder.simple("${in.header." + rule.header + "} contains \"" + rule.contains + "\"");
        }
      }
      exchange.getIn().setHeader(MainRouteBuilder.PRICEHOOK_TAGGING_RULES_HEADER, rules);
    }
  }
}
