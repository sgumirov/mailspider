package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MyRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 22:52<br/>
 */
public class CompressDetectProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    GenericFile f = (GenericFile) exchange.getIn().getBody();
    //todo
    exchange.getIn().setHeader(MyRouteBuilder.COMPRESS_TYPE, "zip");
  }
}
