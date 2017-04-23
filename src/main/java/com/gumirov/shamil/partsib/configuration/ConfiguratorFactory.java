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

  protected void initDefaultValues(HashMap<String, String> kv) {
    kv.put("email.enabled", "1");
    kv.put("local.enabled", "0");
    kv.put("ftp.enabled", "0");
    kv.put("http.enabled", "0");
    
    kv.put("work.dir", "tmp");
    
    //fully qualified, comma separated list of classes to use, in order of execution
    kv.put("plugins.classes", "");

    kv.put("output.url", "http://im.mad.gd/2.php");

    kv.put("endpoints.config.filename", "target/classes/test_local_endpoints.json");
    kv.put("email.accept.rules.config.filename", "target/classes/email_accept_rules.json");

    kv.put("plugins.config.filename", "target/classes/plugins.json");
    kv.put("idempotent.repo", "idempotent_repo.dat");
    kv.put("email.idempotent.repo", "email_idempotent_repo.dat");
    kv.put("pricehook.tagging.config.filename", "target/classes/emai_pricehook_tagging_rules.json");
    kv.put("max.upload.size", "1024000");

//    kv.put("", "");
  }
}
