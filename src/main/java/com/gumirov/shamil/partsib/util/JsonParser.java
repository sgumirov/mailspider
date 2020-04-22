package com.gumirov.shamil.partsib.util;

import com.google.gson.Gson;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Loads and parses json resource using Gson. Mind using -list methods to parse json arrays.
 * @param <T> class to parse
 */
public class JsonParser<T> {
  private static Gson gson = new Gson();

  public T load(String filename, Class<T> klass)
      throws IOException
  {
    return load(filename, klass, Charset.defaultCharset());
  }

  public T load(String filename, Class<T> klass, String encoding)
      throws IOException
  {
    return load(filename, klass, Charsets.toCharset(encoding));
  }

  public T load(String filename, Class<T> klass, Charset charset)
      throws IOException
  {
    String json = IOUtils.toString(klass.getClassLoader().getResourceAsStream(filename), charset);
    return parse(json, klass);
  }

  public List<T> loadList(String filename, Class<T> klass, Charset charset)
      throws IOException
  {
    String json = IOUtils.toString(klass.getClassLoader().getResourceAsStream(filename), charset);
    return parseList(json, klass);
  }

  public T parse(String json, Class<T> klass) {
    return gson.fromJson(json, klass);
  }

  public List<T> parseList(String json, Class<T> klass) {
    //noinspection unchecked
    return Arrays.asList((T[])gson.fromJson(json, Array.newInstance(klass, 0).getClass()));
  }
}
