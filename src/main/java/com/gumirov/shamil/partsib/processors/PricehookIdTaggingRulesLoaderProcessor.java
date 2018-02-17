package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoaderProvider;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import static com.gumirov.shamil.partsib.MainRouteBuilder.MID;

public class PricehookIdTaggingRulesLoaderProcessor implements Processor {
  private final Logger log = LoggerFactory.getLogger(PricehookIdTaggingRulesLoaderProcessor.class.getSimpleName());
  private String url;
  private PricehookIdTaggingRulesConfigLoaderProvider configProvider;

  public PricehookIdTaggingRulesLoaderProcessor(String url, PricehookIdTaggingRulesConfigLoaderProvider configProvider) {
    this.url = url;
    this.configProvider = configProvider;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (url != null && configProvider != null) {
      List<PricehookIdTaggingRule> rules = configProvider.loadPricehookConfig(url);
      if (rules != null) {
        for (PricehookIdTaggingRule rule : rules) {
          rule.predicate = SimpleBuilder.simple("${in.header." + rule.header + "} contains '" + rule.contains + "'");
        }
        exchange.getIn().setHeader(MainRouteBuilder.PRICEHOOK_TAGGING_RULES_HEADER, rules);
      } else {
        log.warn("["+exchange.getIn().getHeader(MID)+"]"+" Tagging rules were not loaded from url. Aborting exchange.");
        throw new IllegalStateException("Tagging rules were not loaded from url. Aborting exchange.");
      }
    }
  }
}
