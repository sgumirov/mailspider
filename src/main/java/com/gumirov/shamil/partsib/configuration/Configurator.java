package com.gumirov.shamil.partsib.configuration;

import java.util.HashMap;

/**
 * Storage for config key-values
 */
public class Configurator {

  HashMap<String, String> storage = new HashMap<>();
  
  Configurator() {
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

  public boolean is(String key, boolean defaultValue) {
    String v = get(key);
    if (v == null) return defaultValue;
    return isTrue(v);
  }

  public static boolean isTrue(String v){
    return v != null &&
        ( "1".equals(v) ||
          "true".equalsIgnoreCase(v) ||
          "enabled".equalsIgnoreCase(v) );
  }

  public Configurator set(String key, String val) {
    storage.put(key, val);
    return this;
  }

}
