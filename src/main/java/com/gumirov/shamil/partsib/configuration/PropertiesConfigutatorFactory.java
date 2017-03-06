package com.gumirov.shamil.partsib.configuration;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class PropertiesConfigutatorFactory extends ConfiguratorFactory {
  private Properties props;

  public PropertiesConfigutatorFactory(Properties props) {
    this.props = props;
  }

  @Override
  protected void initDefaultValues(HashMap<String, String> kv) {
    super.initDefaultValues(kv);

    for (final String name: props.stringPropertyNames())
      kv.put(name, props.getProperty(name));
  }
}
