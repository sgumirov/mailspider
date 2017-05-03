package com.gumirov.shamil.partsib.plugins;

import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import org.apache.camel.Exchange;
import org.slf4j.Logger;

import java.io.InputStream;

/**
 * Sample plugin showing how to change file name.
 */
public class NameChangerPlugin implements Plugin {
  @Override
  public InputStream processFile(FileMetaData metadata, Logger log) {
    //read filename:
    String filename = (String)metadata.headers.get(Exchange.FILE_NAME);

    log.info("processFile() f = "+filename);
    
    //warning: value could be null
    if (filename != null) filename = filename + ".csv";

    //write filename:
    metadata.headers.put(Exchange.FILE_NAME, filename);
    
    return null;
  }
}
