package com.gumirov.shamil.partsib.plugins;

import org.apache.camel.Exchange;
import org.slf4j.Logger;

import java.io.InputStream;

/**
 * Created by phoenix on 2/27/17.
 */
public class NoOpPlugin implements Plugin {
  @Override
  public InputStream processFile(FileMetaData metadata, Logger log) {
    log.info("processFile() f = "+metadata.headers.get(Exchange.FILE_NAME));
    return null;
  }
}
