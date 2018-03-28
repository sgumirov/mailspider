package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gumirov.shamil.partsib.MainRouteBuilder.MID;

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
    } else {
      log.warn("[%s] Output(): NOT SENDING file %s from route id=%s with no pricehook_id",
          exchange.getIn().getHeader(MID), exchange.getExchangeId(), filename, endpointId);
      return;
    }
    log.info(String.format("[%s] Output(): file %s from route name=%s with pricehook_id=%s",
        exchange.getIn().getHeader(MID),
        filename, endpointId, pricehookId));
    byte[] b = exchange.getIn().getBody(byte[].class);
    if (!new HttpPostFileSender(url).onOutput(filename, pricehookId, b, b.length, MainRouteBuilder.MAX_UPLOAD_SIZE))
      throw new Exception(String.format("[%s] File %s was not sent properly, please refer to HttpClient logs",
          exchange.getIn().getHeader(MID), filename));
  }
}
