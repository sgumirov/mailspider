package com.gumirov.shamil.partsib.converters;

import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 22/2/2017 Time: 16:28<br/>
 */
public class InputStreamToGeneericFileConverter implements TypeConverter {

  @Override
  public boolean allowNull() {
    return false;
  }

  @SuppressWarnings("unchecked")
  public <T> T convertTo(Class<T> type, Object value) {
    return null;
  }

  public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {
    // this method with the Exchange parameter will be preferred by Camel to invoke
    // this allows you to fetch information from the exchange during conversions
    // such as an encoding parameter or the likes
    return convertTo(type, value);
  }

  public <T> T mandatoryConvertTo(Class<T> type, Object value) {
    return convertTo(type, value);
  }

  public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) {
    return convertTo(type, value);
  }

  @Override
  public <T> T tryConvertTo(Class<T> type, Object value) {
    return null;
  }

  @Override
  public <T> T tryConvertTo(Class<T> type, Exchange exchange, Object value) {
    return null;
  }
}
