package com.gumirov.shamil.partsib;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class SentDateParserUnitTest {
  @Test
  public void testDateParser() throws ParseException {
    Date d = MainRouteBuilder.mailDateFormat.parse(DeleteOldMailDateParserTest.badDates[0]);
    assert(d.compareTo(DeleteOldMailDateParserTest.correctDates[0])==0);
  }
}
