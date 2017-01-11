package com.gumirov.shamil.partsib.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 11/1/2017 Time: 01:52<br/>
 */
public class OutputProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(OutputProcessor.class);

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.info("Output(): size="+exchange.getIn().getAttachmentNames().size());
  }
}
