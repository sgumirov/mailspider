package com.gumirov.shamil.partsib.util;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NamedByteArray extends NamedResource {
  byte[] bytes;

  public NamedByteArray(String name, byte[] bytes) {
    super(name, bytes.length);
    this.bytes = bytes;
  }

  public byte[] getBytes() {
    return bytes;
  }
}
