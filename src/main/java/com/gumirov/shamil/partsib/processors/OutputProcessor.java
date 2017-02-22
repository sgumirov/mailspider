package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.endpoints.OutputSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 11/1/2017 Time: 01:52<br/>
 */
public class OutputProcessor implements Processor {
  static Logger log = LoggerFactory.getLogger(OutputProcessor.class);
  private Configurator config;

  public OutputProcessor(Configurator config) {
    this.config = config;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    log.info("Output(): file from SOURCE="+exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER));
    String filename = exchange.getIn().getBody(GenericFile.class).getFileName();
    long len = exchange.getIn().getBody(GenericFile.class).getFileLength();
    InputStream is = exchange.getIn().getBody(InputStream.class);
    new OutputSender(config.get("output.url")).onOutput(filename, is, len);
  }
}
