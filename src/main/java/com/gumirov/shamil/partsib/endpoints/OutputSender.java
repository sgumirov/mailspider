package com.gumirov.shamil.partsib.endpoints;

import org.apache.camel.Consume;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Header;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:20<br/>
 */
public class OutputSender {
  @Consume(uri = "direct:output")
  public void onOutput(String body, @Header("filename") String filename)
  {

  }
}
