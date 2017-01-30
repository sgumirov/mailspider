package com.gumirov.shamil.partsib.configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage for config key-values
 */
public class Configurator {

  HashMap<String, String> storage = new HashMap<>();
  //todo set this via beans annotation for mocks
  public static ConfiguratorFactory factory = new ConfiguratorFactory();

  public Configurator() {
  }

  public HashMap<String, String> getStorage() {
    return storage;
  }

  public void setStorage(HashMap<String, String> storage) {
    this.storage = storage;
  }

  public String get(String key){
    return storage.get(key);
  }

  public boolean is(String key) {
    return
        "1".equals(get(key)) ||
        "true".equalsIgnoreCase(get(key)) ||
        "enabled".equalsIgnoreCase(get(key))
        ;
  }

  public Map getJsonAsMap(String key) {

  }
}
