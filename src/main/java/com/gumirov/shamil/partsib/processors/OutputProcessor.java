package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.util.OutputSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 11/1/2017 Time: 01:52<br/>
 */
public class OutputProcessor implements Processor {
  static Logger log = LoggerFactory.getLogger(OutputProcessor.class);
  private String url;

  public OutputProcessor(String url) {
    this.url = url;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME).toString();
    log.info(String.format("Output(): file %s from route id=%s", filename, exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER)));
    InputStream is = exchange.getIn().getBody(InputStream.class);
    new OutputSender(url).onOutput(filename, is);
  }
}
