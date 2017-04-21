package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Expects pricehook id tagging rules to be provided via camel exchange. Ptherwise uses default config provided via
 * constructor.
 */
public class PricehookTaggerProcessor implements Processor {
  List<PricehookIdTaggingRule> rules;

  /**
   * @param rules default rules
   * @throws IOException
   */
  public PricehookTaggerProcessor(List<PricehookIdTaggingRule> rules) throws IOException {
    this.rules = rules;
    for (PricehookIdTaggingRule rule : rules) {
      rule.predicate = SimpleBuilder.simple("${in.header."+rule.header+"} contains \""+rule.contains+"\"");
    }
  }


  @Override
  public void process(Exchange exchange) throws Exception {
    for (PricehookIdTaggingRule rule : rules){
      if (rule.predicate.matches(exchange)) {
        exchange.getIn().setHeader(MainRouteBuilder.PRICEHOOK_ID_HEADER, rule.pricehookid);
      }
    }
  }
}
