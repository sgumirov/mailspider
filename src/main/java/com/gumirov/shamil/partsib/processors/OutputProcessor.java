package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.*;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 11/1/2017 Time: 01:52<br/>
 */
public class OutputProcessor implements Processor {
  private static Logger log = LoggerFactory.getLogger(OutputProcessor.class);
  private String url;

  public OutputProcessor(String url) {
    this.url = url;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME).toString();
    String endpointId = exchange.getIn().getHeader(ENDPOINT_ID_HEADER).toString();
    String pricehookId;
    if (exchange.getIn().getHeader(PRICEHOOK_ID_HEADER) != null) {
      pricehookId = exchange.getIn().getHeader(PRICEHOOK_ID_HEADER).toString();
    } else {
      log.warn("[%s] Output(): NOT SENDING file %s from route id=%s with no pricehook_id",
          exchange.getIn().getHeader(MESSAGE_ID_HEADER), exchange.getExchangeId(), filename, endpointId);
      return;
    }
    log.info(String.format("[%s] Output(): file %s from route name=%s with pricehook_id=%s",
        exchange.getIn().getHeader(MESSAGE_ID_HEADER),
        filename, endpointId, pricehookId));

    InputStream is = exchange.getIn().getBody(InputStream.class);
    int len = ((Number) exchange.getIn().getHeader(LENGTH_HEADER)).intValue();
    if (!new HttpPostFileSender(url).send(filename, pricehookId, is, len, MainRouteBuilder.MAX_UPLOAD_SIZE,
        (String)exchange.getIn().getHeader(MESSAGE_ID_HEADER), (String)exchange.getIn().getHeader(INSTANCE_ID),
        endpointId))
      throw new Exception(String.format("[%s] File %s was not sent properly, please refer to HttpClient logs",
          exchange.getIn().getHeader(MESSAGE_ID_HEADER), filename));
  }
}
