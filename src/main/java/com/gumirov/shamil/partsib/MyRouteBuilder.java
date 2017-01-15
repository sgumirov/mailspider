package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.processors.*;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

  public static final String COMPRESSED_TYPE_HEADER_NAME = "compress.type";
  public static final String ID_HEADER_NAME = "ID_HEADER";

  public void configure() {

    //debug
    getContext().setTracing(Boolean.TRUE);

    CompressDetectProcessor comprDetect = new CompressDetectProcessor();
    UnpackerProcessor unpack = new UnpackerProcessor();
    OutputProcessor outputEndpoint = new OutputProcessor(); //todo
    PluginsProcessor pluginsProcessor = new PluginsProcessor();

//FTP test
//    from("ftp://192.168.50.55/home/pi/1?username=pi&password=gfhjkm&binary=true&passiveMode=true&runLoggingLevel=TRACE").
    //to("direct:packed")

//email test
//        from("imaps://sh.roller:gfhjkm12@imap.mail.ru?consumer.delay=10000&delete=false").
//                log("new email");

//file test

    from("file:src/data/files/?runLoggingLevel=TRACE&delete=false&noop=true").
        process(new SourceIdSetterProcessor("ID-1")).
        to("direct:packed");

    //unpack?
    from("direct:packed").
        process(comprDetect).
        choice().
          when(header(COMPRESSED_TYPE_HEADER_NAME).isNotNull()).
            process(unpack).
            to("direct:unpacked").
          otherwise().
            to("direct:unpacked").
        endChoice();

    //call plugins here
    from("direct:unpacked").
        process(pluginsProcessor).
        to("direct:output");

    //call output procedure
    from("direct:output").
        process(outputEndpoint).
        end();

//        errorHandler(loggingErrorHandler("mylogger.name").level(LoggingLevel.DEBUG)).
//        log("new ftp file").process(new PluginsProcessor()).log("Processed");
  }
}


