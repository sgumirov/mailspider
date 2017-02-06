package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.plugins.TaskContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;

import java.io.File;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 00:26<br/>
 */
public class FileProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    GenericFile o = (GenericFile) exchange.getIn().getBody();
    exchange.getIn().setHeader(MailSpiderRouteBuilder.FILENAME, o.getFileName());
    System.out.println(o);
  }
}
