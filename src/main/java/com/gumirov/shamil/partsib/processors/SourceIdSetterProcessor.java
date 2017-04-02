package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Created by phoenix on 1/15/17.
 */
public class SourceIdSetterProcessor implements Processor {
  private String id;

  public SourceIdSetterProcessor(String id) {
    this.id = id;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    exchange.getIn().setHeader(MainRouteBuilder.ENDPOINT_ID_HEADER, id);
  }
}
