package com.gumirov.shamil.partsib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

  public static byte[] readFully(InputStream is) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] b = new byte[10240];
    int i;
    while ((i=is.read(b))!=-1){
      bos.write(b, 0, i);
    }
    return bos.toByteArray();
  }
}
