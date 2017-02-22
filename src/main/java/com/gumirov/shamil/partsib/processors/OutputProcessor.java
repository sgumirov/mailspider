package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.endpoints.OutputSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 11/1/2017 Time: 01:52<br/>
 */
public class OutputProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(OutputProcessor.class);
  private Configurator config;

  public OutputProcessor(Configurator config) {
    this.config = config;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.info("Output(): file from SOURCE="+exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER));
    String filename = (String) exchange.getIn().getHeader(MailSpiderRouteBuilder.FILENAME);
    if (filename == null) throw new RuntimeException("[OutputProcessor] Error: filename is null");
    if (!new File(filename).exists()) throw new RuntimeException("[OutputProcessor] Error: cannot find file to send: "+filename);
    new OutputSender(config.get("output.url")).onOutput(filename);
  }
}
