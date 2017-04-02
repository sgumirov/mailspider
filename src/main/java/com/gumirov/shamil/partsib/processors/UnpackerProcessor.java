package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:05<br/>
 */
public class UnpackerProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(UnpackerProcessor.class);

  @Override
  public void process(Exchange exchange) throws Exception {
    String format = String.valueOf(exchange.getIn().getHeader(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME));
    String id = String.valueOf(exchange.getIn().getHeader(MainRouteBuilder.ENDPOINT_ID_HEADER));
    
    logger.info("Unpack(). Format="+format+" id="+id);
  }
}
