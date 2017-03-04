package com.gumirov.shamil.partsib.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenix on 2/27/17.
 */
public class PluginsLoader {
  static Logger logger = LoggerFactory.getLogger(PluginsLoader.class);
  ArrayList<Plugin> plugins = new ArrayList<>(); 
  
  public PluginsLoader(String pluginsConfigFile) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = IOUtils.toString(new FileInputStream(pluginsConfigFile), Charset.defaultCharset());
      List<String> classes = mapper.readValue(json, new TypeReference<List<String>>(){});
      for (String c : classes){
        Plugin p = (Plugin) Class.forName(c).newInstance();
        plugins.add(p);
      }
      logger.info(String.format("loaded %d plugins", plugins.size()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ArrayList<Plugin> getPlugins() {
    return plugins;
  }
}
