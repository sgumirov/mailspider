package com.gumirov.shamil.partsib.util;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NamedResource {
  String name;
  int length;

  NamedResource(String name, int length) {
    this.name = name;
    this.length = length;
  }

  public String getName() {
    return name;
  }

  public int getLength() {
    return length;
  }
}
