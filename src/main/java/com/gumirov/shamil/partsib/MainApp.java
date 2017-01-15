package com.gumirov.shamil.partsib;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A Camel Application
 */
public class MainApp {

  /**
   * A main() so we can easily run these routing rules in our IDE
   */
  public static void main(String... args) throws Exception {
//        Main main = new Main();
//        main.addRouteBuilder(new MyRouteBuilder());
//        main.run(args);

    //todo use spring collections to fill config
//    ApplicationContext bcontext = new ClassPathXmlApplicationContext("Beans.xml");
//    bcontext.


    CamelContext context = new DefaultCamelContext();
    context.addRoutes(new MyRouteBuilder());
    context.start();
    Thread.sleep(10000);
    context.stop();
  }

}

