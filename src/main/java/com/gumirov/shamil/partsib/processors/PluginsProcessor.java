package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;

/**
 */
public class PluginsProcessor implements Processor {
  private List<Plugin> plugins;

  public PluginsProcessor(List<Plugin> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    System.out.println("id="+exchange.getExchangeId());
    FileMetaData mdata = new FileMetaData(
        exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER).toString(),
        exchange.getIn().getHeader(MailSpiderRouteBuilder.FILENAME).toString());
    for (Plugin plugin : plugins) {
      plugin.processFile(mdata);
    }
  }
}
