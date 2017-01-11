package com.gumirov.shamil.partsib.processors;

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
    //todo unpack
    logger.info("Unpack()");
  }
}
