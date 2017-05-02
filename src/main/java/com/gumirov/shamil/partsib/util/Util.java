package com.gumirov.shamil.partsib.util;

import java.io.*;
import java.net.URLEncoder;
import java.util.Map;

import static java.lang.String.format;

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

  public static String formatParameters(Map<String, String> parameters){
    if (parameters == null || parameters.size() == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (String k : parameters.keySet()){
      try {
        sb.append('&').append(k).append('=').append(URLEncoder.encode(parameters.get(k), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(format("Cannot encode parameter value of KV pair: %s='%s'", k, parameters.get(k)), e);
//        log.error(format("Cannot encode parameter value of KV pair: %s='%s'", k, parameters.get(k)));
      }
    }
    return sb.toString();
  }
}
