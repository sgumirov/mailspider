package com.gumirov.shamil.partsib.util;

import java.io.InputStream;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class NamedStream extends NamedResource {
  InputStream is;

  public NamedStream(String name, InputStream is, int length) {
    super(name, length);
    this.is = is;
  }

  public InputStream getStream() {
    return is;
  }
}
