package com.gumirov.shamil.partsib.configuration;

import java.util.HashMap;

/**
 * This is for creating mocks
 * Created by phoenix on 1/15/17.
 */
public class ConfiguratorFactory {
  public ConfiguratorFactory() {
  }

  private Configurator c;

  public Configurator getConfigurator(){
    //ApplicationContext bcontext = new ClassPathXmlApplicationContext("Beans.xml");
    c = new Configurator();
    HashMap<String,String> kv = new HashMap<>();
    initDefaultValues(kv);
    c.setStorage(kv);
    return c;
  }

  private void initDefaultValues(HashMap<String, String> kv) {
    kv.put("email.enabled", "false");
    kv.put("local.enabled", "false");
    kv.put("ftp.enabled", "1");
    kv.put("http.enabled", "false");
    
    //fully qualified, comma separated list of classes to use, in order of execution
    kv.put("plugins.classes", "");
//    kv.put("", "");
  }
}
