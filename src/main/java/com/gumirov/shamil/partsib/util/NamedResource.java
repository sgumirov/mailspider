package com.gumirov.shamil.partsib.util;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NamedResource {
  String name;
  long length;

  NamedResource(String name, long length) {
    this.name = name;
    this.length = length;
  }

  public String getName() {
    return name;
  }

  public long getLength() {
    return length;
  }
}
