package com.gumirov.shamil.partsib.plugins;

import java.io.InputStream;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 22/2/2017 Time: 01:04<br/>
 */
public class FileMetaData {
  public final String senderId;
  public final String filename;
  public InputStream is;

  public FileMetaData(String senderId, String filename, InputStream is) {
    this.senderId = senderId;
    this.filename = filename;
    this.is = is;
  }

  @Override
  public String toString() {
    return "FileMetaData{" +
        "senderId='" + senderId + '\'' +
        ", filename='" + filename + '\'' +
        '}';
  }
}
