package com.gumirov.shamil.partsib.plugins;

import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.Exchange;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by phoenix on 2/27/17.
 */
public class NoOpPlugin implements Plugin {
  @Override
  public InputStream processFile(FileMetaData metadata, Logger log) {
    log.info("processFile() f = " + metadata.headers.get(Exchange.FILE_NAME));
//    if (log.isDebugEnabled()) {
      ByteArrayInputStream bas;
      try {
        byte[] b = Util.readFully(metadata.is);
        bas = new ByteArrayInputStream(b);
        log.info("processFile() body = " + new String(b, "UTF-8"));
        return bas;
      } catch (IOException e) {
        log.error("IOException while dealing with attachment in NoOpPlugin");
      }
//    }
    return null;
  }
}
