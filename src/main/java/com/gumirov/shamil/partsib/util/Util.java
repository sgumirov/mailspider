package com.gumirov.shamil.partsib.util;

import org.apache.camel.Message;

import java.io.*;
import java.net.URLEncoder;
import java.util.Map;

import static java.lang.String.format;

/**
 * Created by phoenix on 1/15/17.
 */
public class Util {
  private static final String FILE_PREFIX = "mailspider_";

  /**
   * Reads stream up to end or arr length, whichever less.
   * @param is to read from
   * @param arr to read into
   * @return bytes read actually
   */
  public static int readFully(InputStream is, byte[] arr) throws IOException {
    if (arr == null) throw new IllegalArgumentException("Arr parameter must not be null");
    if (arr.length == 0) return 0;
    int i = 0, arrl = arr.length, r = 0;
    while (i < arrl && r != -1){
      i += r;
      r = is.read(arr, i, arrl - i);
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

  private static long counter = 0;
  /**
   * Calculates Message hash for logs.
   * @param msg incoming to calc hash for
   * @return message hash
   */
  public static String getMID(Message msg) {
//    ++counter;
    return msg.getMessageId();
  }

  public static String removeSensitiveData(String url, String field) {
    if (url.contains(field)){
      return url.substring(0, url.indexOf(field)+field.length()+1)+
          url.substring(url.indexOf("&", url.indexOf(field)+1));
    }
    else return url;
  }

  public static void pipe(InputStream is, OutputStream os, int buf) throws IOException {
    byte[] b = new byte[buf];
    int i;
    while ((i = is.read(b)) != -1) {
      os.write(b, 0, i);
    }
  }

  /**
   * Dumps stream bytes into newly created temporary {@link File}.
   * <p>Note that this method does not call {@link File#deleteOnExit()} on created {@link File}.</p>
   * @param is {@link InputStream} to read bytes from
   * @return newly created temporary file
   * @throws IOException in case of cannot create temporary file or cannot read from InputStream
   */
  public static File dumpTemp(InputStream is) throws IOException {
    File f = File.createTempFile(FILE_PREFIX, null);
    FileOutputStream fos = new FileOutputStream((f));
    byte[] buf = new byte[1024000]; //1Mb
    int i;
    while ((i=is.read(buf)) != -1) {
      fos.write(buf, 0, i);
    }
    fos.flush();
    fos.close();
    return f;
  }

  // TODO write javadoc
  public static File dumpTemp(byte[] b) throws IOException {
    File f = File.createTempFile(FILE_PREFIX, null);
    FileOutputStream fos = new FileOutputStream((f));
    fos.write(b);
    fos.flush();
    fos.close();
    return f;
  }
}
