package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.OutputSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    String endpointId = exchange.getIn().getHeader(MainRouteBuilder.ENDPOINT_ID_HEADER).toString();
    String pricehookId = null;
    if (exchange.getIn().getHeader(MainRouteBuilder.PRICEHOOK_ID_HEADER) != null) {
      pricehookId = exchange.getIn().getHeader(MainRouteBuilder.PRICEHOOK_ID_HEADER).toString();
    }
    else {
      pricehookId = endpointId;
    }
    log.info(String.format("Output(): file %s from route id=%s with pricehook_id=%s",
        filename, endpointId, pricehookId));
    byte[] b = exchange.getIn().getBody(byte[].class);
    if (!new OutputSender(url).onOutput(filename, pricehookId, b, b.length, MainRouteBuilder.MAX_UPLOAD_SIZE))
      throw new RuntimeException("File was not sent properly, this is please refer to HttpClient logs above");
  }
}
