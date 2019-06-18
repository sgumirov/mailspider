/*
 * Copyright (c) 2018 by Shamil Gumirov <shamil@gumirov.com>. All rights are reserved.
 */

package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.ENDPOINT_ID_HEADER;
import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.MESSAGE_ID_HEADER;

/**
 * NOTE: We write to log and mark as SUCCESS in case of any error (exception) happened and rolling back to original 
 * content. 
 */
public class PluginsProcessor implements Processor {
  private static Logger log = LoggerFactory.getLogger(PluginsProcessor.class);
  private List<Plugin> plugins;

  public PluginsProcessor(List<Plugin> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void process(Exchange exchange) {
    exchange.getIn().setHeader(MainRouteBuilder.PLUGINS_STATUS_OK, Boolean.TRUE);
//    log.info("PLUGINS: id="+exchange.getExchangeId()+" file="+exchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
    if (plugins == null || plugins.size() == 0) {
      log.info("No plugins loaded");
      return;
    }
    Plugin last = null;
    try {
      FileMetaData metadata = new FileMetaData(
          exchange.getIn().getHeader(ENDPOINT_ID_HEADER).toString(),
          exchange.getIn().getHeader(Exchange.FILE_NAME).toString(),
          exchange.getIn().getBody(InputStream.class),
          exchange.getIn().getHeaders());
      ArrayList<File> filesToDelete = new ArrayList<>(); //can contain Files and Strings
      for (Plugin plugin : plugins) {
        last = plugin;
        Plugin.Result res = plugin.processFile(metadata, LoggerFactory.getLogger(plugin.getClass().getSimpleName()));
        if (res != null && res.getResult() != null) {
          log.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Plugin "+plugin.getClass().getSimpleName()+" CHANGED file: "+metadata.filename);
          metadata.is = res.getInputStream();
          if (metadata.headers.containsKey(FileMetaData.TEMP_FILE_HEADER)) {
            for (File f : (List<File>) metadata.headers.get(FileMetaData.TEMP_FILE_HEADER)) {
              filesToDelete.add(f);
            }
          }
          if (res.getResult() instanceof File)
            exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.LENGTH_HEADER, ((File)res.getResult()).length());
          else
            log.warn("Cannot set length: Plugin result not a File for plugin="+plugin.getClass().getSimpleName());
        } else {
          log.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Plugin "+plugin.getClass().getSimpleName()+" DID NOT CHANGE file: "+metadata.filename);
        }
        if (metadata.headers != null) {
          exchange.getIn().getHeaders().putAll(metadata.headers);
        } else {
          log.warn("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Plugin MUST NOT return null headers: "+plugin.getClass().getSimpleName());
        }
      }
      exchange.getIn().setBody(metadata.is);
      exchange.getIn().setHeader(MainRouteBuilder.PLUGINS_STATUS_OK, Boolean.TRUE);
      for (File f : filesToDelete) {
        if (!f.delete()) {
          log.warn("PluginProcessor: problem while deleting temp plugin files: cannot delete file=%s", f.getAbsolutePath());
          f.deleteOnExit();
        } else log.info("PluginProcessor: temp file deleted name=" + f.getAbsolutePath());
      }
    } catch (Exception e) {
      log.error("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Error for file="+exchange.getIn().getHeader(Exchange.FILE_NAME, String.class)+" in plugin="+last.getClass().getSimpleName()+". ABORTING transaction marking it as SUCCESS (we will NOT process same incoming again). Please manual process this. Exception = "+e.getMessage(), e);
      exchange.getIn().setHeader(MainRouteBuilder.PLUGINS_STATUS_OK, Boolean.FALSE);
    }
  }
}
