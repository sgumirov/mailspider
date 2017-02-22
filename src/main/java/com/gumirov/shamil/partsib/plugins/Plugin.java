package com.gumirov.shamil.partsib.plugins;

import java.io.InputStream;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 22/2/2017 Time: 01:02<br/>
 */
public interface Plugin {
  /**
   * Writes new file and returns inputstream to read it.
   * @param metadata in params, see {@link FileMetaData} class for details
   * @return can be null, if no new file is produced
   */
  InputStream processFile(FileMetaData metadata);
}
