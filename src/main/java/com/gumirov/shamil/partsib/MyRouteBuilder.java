package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.endpoints.OutputEndpoint;
import com.gumirov.shamil.partsib.processors.CompressDetectProcessor;
import com.gumirov.shamil.partsib.processors.PluginsProcessor;
import com.gumirov.shamil.partsib.processors.UnpackerProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

  public static final String COMPRESS_TYPE = "compress.type";

  public void configure() {
//        from("imaps://sh.roller:gfhjkm12@imap.mail.ru?consumer.delay=10000&delete=false").
//                log("new email");

    //debug
    getContext().setTracing(true);

    CompressDetectProcessor comprDetect = new CompressDetectProcessor();
    UnpackerProcessor unpack = new UnpackerProcessor();
    OutputEndpoint outputEndpoint = new OutputEndpoint(); //todo
    PluginsProcessor pluginsProcessor = new PluginsProcessor();

    //FTP test
//    from("ftp://192.168.50.55/home/pi/1?username=pi&password=gfhjkm&binary=true&passiveMode=true&runLoggingLevel=TRACE").
    //to("direct:packed")

    //file test
    from("file:///j:/java/projects/mailspider/target/files/?runLoggingLevel=TRACE").
        to("direct:packed");

    from("direct:packed").
        process(comprDetect).
        choice().
          when(header(COMPRESS_TYPE).isNotNull()).process(unpack).to("direct:unpacked").
          otherwise().to("direct:unpacked").
        endChoice();

    from("direct:unpacked").
        process(pluginsProcessor).to("direct:output");

    from("direct:output").
        to(outputEndpoint);

//        errorHandler(loggingErrorHandler("mylogger.name").level(LoggingLevel.DEBUG)).
//        log("new ftp file").process(new PluginsProcessor()).log("Processed");
  }
}


