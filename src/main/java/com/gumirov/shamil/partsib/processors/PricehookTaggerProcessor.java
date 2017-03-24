package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainSpiderRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class PricehookTaggerProcessor implements Processor {
  List<PricehookIdTaggingRule> rules;
  
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
        exchange.getIn().setHeader(MainSpiderRouteBuilder.PRICEHOOK_ID_HEADER, rule.pricehookid);
      }
    }
  }
}
