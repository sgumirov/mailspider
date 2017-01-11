package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MyRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 22:52<br/>
 */
public class CompressDetectProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(CompressDetectProcessor.class);

  @Override
  public void process(Exchange exchange) throws Exception {
    GenericFile f = (GenericFile) exchange.getIn().getBody();
    //todo
    Random r = new Random();
    if (r.nextFloat()>0.5f) {
      exchange.getIn().setHeader(MyRouteBuilder.COMPRESS_TYPE, "zip");
      logger.info("Compr(): zip");
    } else {
      logger.info("Compr(): unpacked");
    }
  }
}
