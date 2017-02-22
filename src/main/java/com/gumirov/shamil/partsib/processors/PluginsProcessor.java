package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 */
public class PluginsProcessor implements Processor {
  static Logger log = LoggerFactory.getLogger(PluginsProcessor.class);
  private List<Plugin> plugins;

  public PluginsProcessor(List<Plugin> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    log.info("id="+exchange.getExchangeId()+" in="+exchange.getIn());
    FileMetaData mdata = new FileMetaData(
        exchange.getIn().getHeader(MailSpiderRouteBuilder.ENDPOINT_ID_HEADER).toString(),
        exchange.getIn().getBody(GenericFile.class).getFileName(),
        exchange.getIn().getBody(InputStream.class));
    for (Plugin plugin : plugins) {
      plugin.processFile(mdata);
      //todo chained inputstream
    }
    //todo return inputstream
  }
}
