package com.gumirov.shamil.partsib.plugins;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 22/2/2017 Time: 01:04<br/>
 */
public class FileMetaData {
  String senderId;
  String filename;

  public FileMetaData(String senderId, String filename) {
    this.senderId = senderId;
    this.filename = filename;
  }
}
