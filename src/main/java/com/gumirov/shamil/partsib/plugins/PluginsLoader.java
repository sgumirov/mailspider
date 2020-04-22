package com.gumirov.shamil.partsib.plugins;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.JsonParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Inititalizes plugins with the default constructor.
 */
public class PluginsLoader {
  private static Logger logger = LoggerFactory.getLogger(PluginsLoader.class);
  private ArrayList<Plugin> plugins = new ArrayList<>();
  
  public PluginsLoader(String pluginsConfigFile) {
    try {
      String[] classes = new JsonParser<String[]>().load(pluginsConfigFile, String[].class, MainRouteBuilder.CHARSET);
      for (String c : classes){
        Plugin p = (Plugin) Class.forName(c).newInstance();
        plugins.add(p);
      }
      if (plugins.size() > 0) logger.info(String.format("Loaded %d plugins", plugins.size()));
      else logger.warn("Warning: NO PLUGINS LOADED! Config path: "+pluginsConfigFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ArrayList<Plugin> getPlugins() {
    return plugins;
  }
}
