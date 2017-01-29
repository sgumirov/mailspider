package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.processors.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.dataformat.zipfile.ZipSplitter;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ID_HEADER_NAME = "ID_HEADER";
  public static enum CompressorType {
    GZIP, ZIP, RAR
  }

  public ConfiguratorFactory confFactory = new ConfiguratorFactory();

  public void configure() {

    //debug
    getContext().setTracing(Boolean.TRUE);

    Configurator config = confFactory.getConfigurator();

    CompressDetectProcessor comprDetect = new CompressDetectProcessor();
    UnpackerProcessor unpack = new UnpackerProcessor(); //todo
    OutputProcessor outputEndpoint = new OutputProcessor(); //todo
    PluginsProcessor pluginsProcessor = new PluginsProcessor(); //todo
    EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();

    SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();
    ZipSplitter zipSplitter = new ZipSplitter();

//FTP test
//    from("ftp://192.168.50.55/home/pi/1?username=pi&password=gfhjkm&binary=true&passiveMode=true&runLoggingLevel=TRACE").
    //to("direct:packed")

//email test
    if ("1".equals(config.get("email.enabled"))) {
      from("imaps://imap.mail.ru?password=gfhjkm12&username=sh.roller%40mail.ru&consumer.delay=10000&delete=false").
              split(splitEmailExpr).
              process(emailAttachmentProcessor).
              to("direct:unpacked").
              end();
    }

//file test

//    from("file:src/data/files/?runLoggingLevel=TRACE&delete=false&noop=true").
//        process(new SourceIdSetterProcessor("ID-1")).
//        to("direct:packed");

    //unpack?
    from("direct:packed").
        process(comprDetect).
        choice().
          when(header(COMPRESSED_TYPE_HEADER_NAME).isEqualTo(CompressorType.ZIP)).
              split(zipSplitter).streaming().to("direct:unpacked").
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


