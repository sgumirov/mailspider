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

  Configurator() {
  }

  public HashMap<String, String> getStorage() {
    return storage;
  }

  public void setStorage(HashMap<String, String> storage) {
    this.storage = storage;
  }

  public String get(String key){
    return get(key, null);
  }

  public String get(String key, String defVal){
    if (!storage.containsKey(key)) return defVal;
    return storage.get(key);
  }

  public boolean is(String key) {
    String v = get(key);
    if (v == null) return false;
    return isTrue(v);
  }

  public static boolean isTrue(String v){
    return "1".equals(v) ||
           "true".equalsIgnoreCase(v) ||
           "enabled".equalsIgnoreCase(v);
  }
}
