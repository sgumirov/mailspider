package com.gumirov.shamil.partsib.plugins;

import org.slf4j.Logger;

import java.io.InputStream;

/**
 * Created by phoenix on 2/27/17.
 */
public class NoOpPlugin implements Plugin {
  @Override
  public InputStream processFile(FileMetaData metadata, Logger log) {
    log.info("NoOpPlugin: processFile() f = "+metadata);
    return null;
  }
}
