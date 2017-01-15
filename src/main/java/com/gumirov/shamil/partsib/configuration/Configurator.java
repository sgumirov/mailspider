package com.gumirov.shamil.partsib.configuration;

import java.util.HashMap;

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
}
