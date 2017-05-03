package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static com.gumirov.shamil.partsib.MainRouteBuilder.ENDPOINT_ID_HEADER;
/**
 * NOTE: We abort any changes ("rollback") in case of any error (exception) happened and rolling back to original 
 * content. 
 */
public class PluginsProcessor implements Processor {
  static Logger log = LoggerFactory.getLogger(PluginsProcessor.class);
  private List<Plugin> plugins;

  public PluginsProcessor(List<Plugin> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
//    log.info("PLUGINS: id="+exchange.getExchangeId()+" file="+exchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
    if (plugins == null || plugins.size() == 0) {
      log.info("No plugins loaded");
      return;
    }
    Plugin last = null;
    try {
      FileMetaData mdata = new FileMetaData(
          exchange.getIn().getHeader(ENDPOINT_ID_HEADER).toString(),
          exchange.getIn().getHeader(Exchange.FILE_NAME).toString(),
          exchange.getIn().getBody(InputStream.class),
          exchange.getIn().getHeaders());
      for (Plugin plugin : plugins) {
        last = plugin;
        InputStream is = plugin.processFile(mdata, LoggerFactory.getLogger(plugin.getClass().getSimpleName()));
        if (is != null) {
          log.debug("Plugin "+plugin.getClass().getSimpleName()+" CHANGED file: "+mdata.filename);
          mdata.is = is;
        } else {
          log.debug("Plugin "+plugin.getClass().getSimpleName()+" DID NOT file: "+mdata.filename);
        }
      }
      exchange.getIn().setBody(mdata.is);
    } catch (Exception e) {
      log.error("Error occured while plugins execution. ABORTING ALL changes done by ANY plugin in sequence! Error at plugin instance = "+last+" of class = "+last.getClass().getSimpleName()+" with an exception = "+e.getMessage(), e);
    }
  }
}
