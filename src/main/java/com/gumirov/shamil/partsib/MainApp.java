package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.PropertiesConfigutatorFactory;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;

import java.io.InputStream;
import java.util.Properties;

/**
 * A Camel Application
 */
public class MainApp {

  /**
   * A main() so we can easily run these routing rules in our IDE
   */
  public static void main(String... args) throws Exception {
      Main main = new Main();
      Properties config = new Properties();
      InputStream is = MainApp.class.getClassLoader().getResourceAsStream("config.properties");
      config.load(is);
      main.addRouteBuilder(new MainRouteBuilder(new PropertiesConfigutatorFactory(config).getConfigurator()));
      //todo remove add event listener
      main.addMainListener(new EventsListener());
      main.run(args);
  }

  //TODO remove EventsListener
  /**
   * @deprecated 
   */
  private static class EventsListener extends MainListenerSupport {

    @Override
    public void beforeStart(MainSupport main) {
      System.out.println("beforeStart()");
    }

    @Override
    public void afterStop(MainSupport main) {
      System.out.println("afterStop()");
    }
  }
}

