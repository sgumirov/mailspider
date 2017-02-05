package com.gumirov.shamil.partsib.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 15:53<br/>
 //todo load plugins
 */
public class PluginsProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    System.out.println("id="+exchange.getExchangeId());
    
  }
}
