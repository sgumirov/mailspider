package com.gumirov.shamil.partsib.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gumirov.shamil.partsib.MainRouteBuilder.MID;

public class SkipMessageExceptionErrorHandler implements Processor {
  Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void process(Exchange exchange) {
    Exception e = exchange.getException();
    log.info("["+exchange.getIn().getHeader(MID)+"] Skipped email with id="+exchange.getIn().getHeader("Message-ID")+" subj="+exchange.getIn().getHeader("Subject").toString()+" t="+e);
    exchange.getIn().setFault(true);
    exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
  }
}
