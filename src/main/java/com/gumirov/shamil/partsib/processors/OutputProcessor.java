package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
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
    String endpointId = exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER).toString();
    String pricehookId = exchange.getIn().getHeader(MailSpiderRouteBuilder.PRICEHOOK_ID_HEADER).toString();
    if (pricehookId == null) {
      pricehookId = endpointId;
    }
    log.info(String.format("Output(): file %s from route id=%s with pricehook_id=%s",
        filename, endpointId, pricehookId));
    byte[] b = exchange.getIn().getBody(byte[].class);
    if (!new OutputSender(url).onOutput(filename, pricehookId, b, b.length, MailSpiderRouteBuilder.MAX_UPLOAD_SIZE))
      throw new RuntimeException("File was not sent properly, this is please refer to HttpClient logs above");
  }
}
