package com.gumirov.shamil.partsib.mail;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

/**
 * Helps to correctly decode RFC 2047 non compliant headers.
 *
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class HeaderDecoder {
  protected static String regex = "(\\?=)[\\s\\t]*[\\r\\n]*[\\s\\t]*(=\\?(utf-8)\\?[QB]\\?)";

  public static String decode(String v) throws UnsupportedEncodingException {
    String r;
    if ((r = MimeUtility.decodeText(v)).contains("\uFFFD")) {
      return MimeUtility.decodeText(v.replaceAll(regex, ""));
    }
    return r;
  }
}
