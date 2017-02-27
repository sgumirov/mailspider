package com.gumirov.shamil.partsib.plugins;

import org.slf4j.Logger;

import java.io.InputStream;

/**
 * Plugin can change only file contents via returning new InputStream value from processFile(). <p>Note: it's recommended to do 
 * all the operations with streams in memory instead of disk to keep latency low. 
 * <p>(c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 */
public interface Plugin {
  /**
   * Processes file and maybe replaces its content with new one.
   * <p>In case of recoverable error write log entry and return null, in case of fatal error throw exception. Execution 
   * of plugins will then be skipped and rolled back. 
   * @param metadata in params, see {@link FileMetaData} class for details
   * @param log logger to use
   * @return new file contents, or null if no changes needed
   */
  InputStream processFile(FileMetaData metadata, Logger log);
}
