package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.PricehookIdTaggingRulesConfigLoader;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.MESSAGE_ID_HEADER;

public class PricehookIdTaggingRulesLoaderProcessor implements Processor {
  private final Logger log = LoggerFactory.getLogger(PricehookIdTaggingRulesLoaderProcessor.class.getSimpleName());
  private String url;
  private PricehookIdTaggingRulesConfigLoader configLoader;

  public PricehookIdTaggingRulesLoaderProcessor(String url, PricehookIdTaggingRulesConfigLoader configLoader) {
    this.url = url;
    this.configLoader = configLoader;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (url != null && configLoader != null) {
      List<PricehookIdTaggingRule> rules = configLoader.loadPricehookConfig(url, exchange);
      if (rules != null) {
        for (PricehookIdTaggingRule rule : rules) {
          rule.predicate = SimpleBuilder.simple("${in.header." + rule.header + "} contains '" + rule.contains + "'");
        }
        exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.PRICEHOOK_TAGGING_RULES_HEADER, rules);
      } else {
        log.warn("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Tagging rules were not loaded from url. Falling back to default rules");
        //throw new IllegalStateException("Tagging rules were not loaded from url. Aborting exchange.");
      }
    }
  }
}
