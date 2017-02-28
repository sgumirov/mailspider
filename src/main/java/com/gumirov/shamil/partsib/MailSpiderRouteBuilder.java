package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.endpoints.Output;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.plugins.PluginsLoader;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.impl.ProcessorEndpoint;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A Camel Java DSL Router
 */
public class MailSpiderRouteBuilder extends RouteBuilder {
  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ENDPOINT_ID_HEADER = "endpoint.id";
  public static final String BASE_DIR = "base.dir";
  private String workDir = "/tmp";

  public static enum CompressorType {
    GZIP, ZIP, RAR, _7Z
  }

  //@inject
  public ConfiguratorFactory confFactory = new ConfiguratorFactory();
  public Configurator config = confFactory.getConfigurator();

  public MailSpiderRouteBuilder() {}

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
  
  public Endpoints getEndpoints() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(new FileInputStream(config.get("endpoints.config.filename") ), Charset.defaultCharset());
    return mapper.readValue(json, Endpoints.class);
  }

  public ArrayList<EmailRule> getEmailRules() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(new FileInputStream(config.get("email.rules.config.filename") ), Charset.defaultCharset());
    return mapper.readValue(json, new TypeReference<List<EmailRule>>(){});
  }

  public void configure() {
    try {
      //debug
      getContext().setTracing(Boolean.TRUE);

      ArchiveTypeDetectorProcessor comprDetect = new ArchiveTypeDetectorProcessor();
      UnpackerProcessor unpack = new UnpackerProcessor(); //todo add RAR, 7z
      OutputProcessor outputProcessorEndpoint = new OutputProcessor(config.get("output.url"));
      PluginsProcessor pluginsProcessor = new PluginsProcessor(new PluginsLoader(config.get("plugins.config.filename")).getPlugins());
      EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();

      SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();
      ZipSplitter zipSplitter = new ZipSplitter();

      FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(
          config.get("idempotent.repo", "tmp/idempotent_repo.dat"));
      workDir = config.get("work.dir", workDir);
      Endpoints endpoints = getEndpoints();

//FTP <production>
      if (config.is("ftp.enabled")) {
        log.info(String.format("[FTP] Setting up %d source endpoints", endpoints.ftp.size()));
        for (Endpoint ftp : endpoints.ftp) {
          //String ftpUrl = "ftp://127.0.0.1:2021/?username=ftp&password=a@b.com&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false";
          String ftpUrl = ftp.url+"?username="+ftp.user+"&password="+ftp.pwd+"&stepwise=false&binary=true&passiveMode=true&runLoggingLevel=TRACE&delete=false&delay="+ftp.delay;
          String producerId = ftp.id;

          from(ftpUrl).
              setHeader(ENDPOINT_ID_HEADER, constant(producerId)).
              idempotentConsumer(
                  repoMan.createExpression(),
                  FileIdempotentRepository.fileIdempotentRepository(repoMan.getRepoFile(),
                      100000, 102400000)).
              to("direct:packed");
          log.info("FTP source endpoint is added: "+ftp);
        }
      }

//unzip/unrar
      from("direct:packed").
          process(comprDetect).id("CompressorDetector").
          choice().
            when(header(COMPRESSED_TYPE_HEADER_NAME).isEqualTo(CompressorType.ZIP)).
//          unzip does not work!
              split(zipSplitter).streaming().
//              log(LoggingLevel.INFO, "Unzipped file: $simple{in.header.CamelFileName}").
              to("direct:unpacked").endChoice().
  //          when(header(COMPRESSED_TYPE_HEADER_NAME).isNotNull()). //TODO process other archive types
  //            process(unpack).id("UnpackProcessor").
  //            to("direct:unpacked").endChoice().
            otherwise().
//              log(LoggingLevel.INFO, "Plain file: $simple{in.header.CamelFileName}").
              to("direct:unpacked").endChoice().
          end();

//call plugins
      from("direct:unpacked").
          process(pluginsProcessor).id("PluginsProcessor").
          to("direct:output").
          end();

//output send
      from("direct:output").routeId("output1").
          to(new Output(outputProcessorEndpoint)).id("outputprocessor");
/*
        process(outputProcessorEndpoint).id("OutputProcessor").
        end();
*/

//ERROR Handling
//        errorHandler(loggingErrorHandler("mylogger.name").level(LoggingLevel.DEBUG)).
//        log("new ftp file").process(new PluginsProcessor()).log("Processed");

      //email <production>
      if (config.is("email.enabled")) {
        ArrayList<Predicate> negatives = new ArrayList<>();
        Predicate negative = null;
        ArrayList<EmailRule> rules = getEmailRules();
        for (EmailRule rule : rules){
          if (Configurator.isTrue(rule.reject)){
            Predicate p = PredicateBuilder.regex(ExpressionBuilder.headerExpression("From"),
                rule.regexp);
            negatives.add(p);
            if (negative == null) negative = p; else negative = PredicateBuilder.or(p);
          }
        }
        log.info(String.format("[FTP] Setting up %d source endpoints", endpoints.email.size()));
        for (Endpoint email : endpoints.email) {
          //fetchSize=1 1 at a time
          from(String.format("imaps://%s?password=%s&username=%s&consumer.delay=%s&delete=false&fetchSize=1",
              email.url, URLEncoder.encode(email.pwd, "UTF-8"), URLEncoder.encode(email.user, "UTF-8"),
              email.delay)).
            choice().when(negative does nto work).to("direct:rejected").endChoice().otherwise().
            setHeader(ENDPOINT_ID_HEADER, constant(email.id)).
            split(splitEmailExpr).
            process(emailAttachmentProcessor).
            to("direct:packed");
          log.info("Email endpoint is added with id="+email.id);
        }
      }

      from("direct:rejected").routeId("REJECTED_EMAILS").
          to("log:REJECT_MAILS?level=INFO&showAll=true");

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
    } catch (Exception e) {
      log.error("Cannot build route", e);
      throw new RuntimeException("Cannot continue", e);
    }
  }                                      
}


