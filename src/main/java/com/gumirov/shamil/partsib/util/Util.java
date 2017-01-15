package com.gumirov.shamil.partsib.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by phoenix on 1/15/17.
 */
public class Util {
  public static int readFully(InputStream is, byte[] arr) throws IOException {
    if (arr == null || arr.length == 0) return 0;
    int i = 0, arrl = arr.length, r = 0;
    while (i < arrl && r != -1){
      i += r;
      r = is.read(arr, i, arrl-i);
    }
    return i;
  }
}
