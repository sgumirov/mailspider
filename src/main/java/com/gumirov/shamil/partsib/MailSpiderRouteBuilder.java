package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.apache.camel.builder.ExpressionBuilder.append;

/**
 * A Camel Java DSL Router
 */
public class MailSpiderRouteBuilder extends RouteBuilder {

  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ID_HEADER_NAME = "ID_HEADER";
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

  public void configure() throws IOException {
    //debug
//    getContext().setTracing(Boolean.TRUE);


    CompressDetectProcessor comprDetect = new CompressDetectProcessor();
    UnpackerProcessor unpack = new UnpackerProcessor(); //todo
    OutputProcessor outputProcessorEndpoint = new OutputProcessor(config); //todo
    PluginsProcessor pluginsProcessor = new PluginsProcessor(new ArrayList<Plugin>()); //todo
    EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();

    FileNameProcessor fileNameProcessor = new FileNameProcessor();
    SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();
    ZipSplitter zipSplitter = new ZipSplitter();

    FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(); //todo get filename from config
    
    workDir = config.get("work.dir");
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getResourceAsStream( config.get("endpoints.config.filename") ), Charset.defaultCharset());
    Endpoints endpoints = mapper.readValue(json, Endpoints.class);

//FTP <production>
    if (config.is("ftp.enabled")) {
      for (Endpoint ftp : endpoints.ftp) {
        log.info("FTP source endpoint is added: "+ftp);
        //String ftpUrl = "ftp://127.0.0.1:2021/?username=ftp&password=a@b.com&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false";
        String ftpUrl = ftp.url+"?username="+ftp.user+"&password="+ftp.pwd+"&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false";
        String producerId = ftp.id;

        from(ftpUrl).
            setHeader(ENDPOINT_ID_HEADER, constant(producerId)).
            idempotentConsumer(
                append(append(header("CamelFileName"), simple("-")), header("CamelFileLength")),
                FileIdempotentRepository.fileIdempotentRepository(repoMan.getRepoFile(),
                    100000, 102400000)).
            to("file://"+workDir);

        //additional step of copying so that work dir file remains after Exchange is finished

        from("file://"+workDir).
            to("direct:packed");
      }
    }
    
    //TODO: check if stored into file?

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
          process(new SourceIdSetterProcessor("ID-1")).
          to("direct:packed");
    }

//Main work [production]
    from("direct:packed").
        //todo use idempotent consumer here to skip already processed files from ftp!
        //process(fileStorageProcessor).
        //to("file://"+workDir+"/"+header(ENDPOINT_ID_HEADER)+"?fileName="+header(FILENAME)).
        process(comprDetect).
        choice().
          when(header(COMPRESSED_TYPE_HEADER_NAME).isEqualTo(CompressorType.ZIP)).
            split(zipSplitter).streaming().
            to("direct:unpacked").endChoice().
          when(header(COMPRESSED_TYPE_HEADER_NAME).isNotNull()).
            process(unpack).
            to("direct:unpacked").endChoice().
          otherwise().
            to("direct:unpacked").
        endChoice();

    //call plugins here
    from("direct:unpacked").
        process(pluginsProcessor).
        to("direct:output");

    //call output procedure
    from("direct:output").
        setHeader(BASE_DIR, constant(workDir)).
        process(outputProcessorEndpoint).
        end();

//        errorHandler(loggingErrorHandler("mylogger.name").level(LoggingLevel.DEBUG)).
//        log("new ftp file").process(new PluginsProcessor()).log("Processed");
  }
}


