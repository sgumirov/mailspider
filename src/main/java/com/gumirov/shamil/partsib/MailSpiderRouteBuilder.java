package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import static org.apache.camel.builder.ExpressionBuilder.append;

/**
 * A Camel Java DSL Router
 */
public class MailSpiderRouteBuilder extends RouteBuilder {

  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ENDPOINT_ID_HEADER = "endpoint.id";
  public static final String FILENAME = "filename";
  public static final java.lang.String BASE_DIR = "base.dir";
  private String workDir = "/tmp";

  public static enum CompressorType {
    GZIP, ZIP, RAR, _7Z
  }

  //@inject
  public ConfiguratorFactory confFactory = new ConfiguratorFactory();
  public Configurator config = confFactory.getConfigurator();

  public MailSpiderRouteBuilder() {
  }

  public MailSpiderRouteBuilder(Configurator config) {
    this.config = config;
  }

  public MailSpiderRouteBuilder(CamelContext context) {
    super(context);
  }

  public MailSpiderRouteBuilder(CamelContext context, Configurator config) {
    super(context);
    this.config = config;
  }

  public void configure() throws IOException {
    //debug
    getContext().setTracing(Boolean.TRUE);

    CompressDetectProcessor comprDetect = new CompressDetectProcessor();
    UnpackerProcessor unpack = new UnpackerProcessor(); //todo add RAR, 7z
    OutputProcessor outputProcessorEndpoint = new OutputProcessor(config);
    PluginsProcessor pluginsProcessor = new PluginsProcessor(new ArrayList<Plugin>());
    EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();

    FileNameProcessor fileNameProcessor = new FileNameProcessor();
    SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();
    ZipSplitter zipSplitter = new ZipSplitter();

    FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(
        config.get("idempotent.repo", "tmp/idempotent_repo.dat"));
    
    workDir = config.get("work.dir", workDir);
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(new FileInputStream(config.get("endpoints.config.filename") ), Charset.defaultCharset());
    Endpoints endpoints = mapper.readValue(json, Endpoints.class);

//FTP <production>
    if (config.is("ftp.enabled")) {
      log.info(String.format("[FTP] Setting up %d source endpoints", endpoints.ftp.size()));
      for (Endpoint ftp : endpoints.ftp) {
        log.info("FTP source endpoint is added: "+ftp);
        //String ftpUrl = "ftp://127.0.0.1:2021/?username=ftp&password=a@b.com&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false";
        String ftpUrl = ftp.url+"?username="+ftp.user+"&password="+ftp.pwd+"&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false";
        String producerId = ftp.id;

        from(ftpUrl).
            setHeader(ENDPOINT_ID_HEADER, constant(producerId)).
            idempotentConsumer(
                repoMan.createExpression(),
                FileIdempotentRepository.fileIdempotentRepository(repoMan.getRepoFile(),
                    100000, 102400000)).
            to("direct:packed");
      }
    }

    //TODO: check if stored into file?

//Main work [production]
    from("direct:packed").
        process(comprDetect).id("CompressorDetector").
        choice().
          when(header(COMPRESSED_TYPE_HEADER_NAME).isEqualTo(CompressorType.ZIP)).
            split(zipSplitter).streaming().convertBodyTo(GenericFile.class).
            to("direct:unpacked").endChoice().
//          when(header(COMPRESSED_TYPE_HEADER_NAME).isNotNull()). //TODO process other archive types
//            process(unpack).id("UnpackProcessor").
//            to("direct:unpacked").endChoice().
          otherwise().
            to("direct:unpacked").endChoice();

//call plugins
    from("direct:unpacked").
        process(pluginsProcessor).id("PluginsProcessor").
        to("direct:output");

//output send
    from("direct:output").
//        setHeader(BASE_DIR, constant(workDir)).
        process(outputProcessorEndpoint).id("OutputProcessor").
        end();

//ERROR Handling
//        errorHandler(loggingErrorHandler("mylogger.name").level(LoggingLevel.DEBUG)).
//        log("new ftp file").process(new PluginsProcessor()).log("Processed");

    //email <production>
    if (config.is("email.enabled")) {
      //todo configure from Endpoints
      log.info("Email source endpoint is <enabled>");
      //fetchSize=1 1 at a time
      from("imaps://imap.mail.ru?password=gfhjkm12&username=sh.roller%40mail.ru&consumer.delay=10000&delete=false&fetchSize=1").
          setHeader(ENDPOINT_ID_HEADER, constant("mail01")).
          split(splitEmailExpr).
          process(emailAttachmentProcessor).
          to("direct:packed").
          end();
    }

//HTTP <production>
    //TODO

//file <test only>
    //todo remove!
    if (config.is("local.enabled")) {
      from("file:src/data/files/?runLoggingLevel=TRACE&delete=false&noop=true").
          setHeader(ENDPOINT_ID_HEADER, constant("id")). //todo add endpoint id from config
          //process(new SourceIdSetterProcessor("ID-1")).
          to("direct:packed");
    }
  }
}


