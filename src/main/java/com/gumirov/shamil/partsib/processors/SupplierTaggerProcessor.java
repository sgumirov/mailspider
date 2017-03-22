package com.gumirov.shamil.partsib.processors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.SupplierTaggingRule;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 *
 */
public class SupplierTaggerProcessor implements Processor {
  List<SupplierTaggingRule> rules;
  
  public SupplierTaggerProcessor(List<SupplierTaggingRule> rules) throws IOException {
    this.rules = rules;
    for (SupplierTaggingRule rule : rules) {
      rule.predicate = SimpleBuilder.simple("${in.header."+rule.header+"} contains \""+rule.contains+"\"");
    }
  }


  @Override
  public void process(Exchange exchange) throws Exception {
    for (SupplierTaggingRule rule : rules){
      if (rule.predicate.matches(exchange)) {
        exchange.getIn().setHeader(MailSpiderRouteBuilder.SUPPLIER_ID_HEADER, rule.supplierid);
      }
    }
  }
}
