package com.gumirov.shamil.partsib;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;

/**
 * A Camel Application
 */
public class MainApp {

  /**
   * A main() so we can easily run these routing rules in our IDE
   */
  public static void main(String... args) throws Exception {
      Main main = new Main();
      main.addRouteBuilder(new MailSpiderRouteBuilder());
      // add event listener
      main.addMainListener(new EventsListener());
//      main.enableTrace();
      main.run(args);


/*
    CamelContext context = new DefaultCamelContext();
    context.addRoutes(new MailSpiderRouteBuilder());
    context.start();

    Thread.sleep(10000);
    context.stop();
*/
  }

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

