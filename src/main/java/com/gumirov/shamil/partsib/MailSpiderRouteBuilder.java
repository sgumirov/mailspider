package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.factories.RouteFactory;
import com.gumirov.shamil.partsib.plugins.PluginsLoader;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

/**
 * A Camel Java DSL Router
 */
public class MailSpiderRouteBuilder extends RouteBuilder {
  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ENDPOINT_ID_HEADER = "endpoint.id";
  public static final String PRICEHOOK_ID_HEADER = "pricehook.id";
  public static final String BASE_DIR = "base.dir";
  public static int MAX_UPLOAD_SIZE;
//  private String workDir = "/tmp";

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

  public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
    String json = IOUtils.toString(new FileInputStream(config.get("pricehook.tagging.config.filename")), Charset.defaultCharset());
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, new TypeReference<List<PricehookIdTaggingRule>>(){});
  }

  public int getMaxUploadSize(String maxSizeText) {
    return Integer.parseInt(maxSizeText);
  }

  public void configure() {
    try {
      //debug
      getContext().setTracing(Boolean.TRUE);

      ArchiveTypeDetectorProcessor comprDetect = new ArchiveTypeDetectorProcessor();
      UnpackerProcessor unpack = new UnpackerProcessor(); //todo add support RAR, 7z
      OutputProcessor outputProcessorEndpoint = new OutputProcessor(config.get("output.url"));
      PluginsProcessor pluginsProcessor = new PluginsProcessor(new PluginsLoader(config.get("plugins.config.filename")).getPlugins());
      EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();
      List<PricehookIdTaggingRule> pricehookRules = getPricehookConfig();
      PricehookTaggerProcessor pricehookTaggerProcessor = new PricehookTaggerProcessor(pricehookRules);
      MAX_UPLOAD_SIZE = getMaxUploadSize(config.get("max.upload.size", "1024000"));

      SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();
      ZipSplitter zipSplitter = new ZipSplitter();

      FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(
          config.get("idempotent.repo", "tmp/idempotent_repo.dat"));
//      workDir = config.get("work.dir", workDir);
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

      //HTTP <production>
      if (config.is("http.enabled")) {
        log.info(String.format("[HTTP] Setting up %d source endpoints", endpoints.http.size()));
        for (Endpoint http : endpoints.http) {
          
          String startEndpoint = "direct:start"+http.id;
          String producerId = http.id;

          from("timer://http?fixedRate=true&period="+http.delay).
                  setHeader(ENDPOINT_ID_HEADER, constant(producerId)).
              to(startEndpoint).
              end();

          RouteFactory factory = (RouteFactory) Class.forName(http.factory).newInstance();
          factory.setStartSubroute(startEndpoint);
          factory.setEndSubrouteSuccess("direct:httpidempotent");
          factory.setUrl(http.url);
          factory.setUser(http.user);
          factory.setPasswd(http.pwd);

          RouteBuilder builder = factory.createRouteBuilder();
          builder.addRoutesToCamelContext(getContext());

          from("direct:httpidempotent").
              /*idempotentConsumer(
                  repoMan.createExpression(),
                  FileIdempotentRepository.fileIdempotentRepository(repoMan.getRepoFile(),
                      100000, 102400000)).*/
              to("direct:packed").
              end();
          log.info("HTTP source endpoint is added: "+http);
        }
      }

//unzip/unrar
      from("direct:packed").
          process(comprDetect).id("CompressorDetector").
          choice().
            when(header(COMPRESSED_TYPE_HEADER_NAME).isEqualTo(CompressorType.ZIP)).
              split(zipSplitter).
              //streaming(). //seems we don't need this
              to("direct:unpacked").endChoice().
            when(header(COMPRESSED_TYPE_HEADER_NAME).isNotNull()).
              split(beanExpression(new UnpackerSplitter(), "unpack")).
              to("direct:unpacked").endChoice().
            otherwise().
              to("direct:unpacked").endChoice().
          end();

//call plugins
      from("direct:unpacked").
          process(pluginsProcessor).id("PluginsProcessor").
          to("direct:output").end();

//dead letter channel:
      from("direct:deadletter").
          to("log:DeadLetterChannel?level=DEBUG&showAll=true").
          end();

//output send
      from("direct:output").
          routeId("output").
          process(outputProcessorEndpoint).id("outputprocessor").
          end();

      //email <production>
      if (config.is("email.enabled")) {
        final List<Predicate> predicatesAnyTrue = new ArrayList<>();
        ArrayList<EmailRule> rules = getEmailRules();
        for (EmailRule rule : rules){
          predicatesAnyTrue.add(SimpleBuilder.simple("${in.header."+rule.header+"} contains \""+rule.contains+"\""));
        }

        final Predicate anyTruePredicateSet = new Predicate() {
          @Override
          public boolean matches(Exchange exchange) {
            for (Predicate p : predicatesAnyTrue){
              if (p.matches(exchange)) {
                return true;
              }
            }
            return false;
          }
        };

        log.info(String.format("[EMAIL] Setting up %d source endpoints", endpoints.email.size()));
        for (Endpoint email : endpoints.email) {
          //fetchSize=1 1 at a time
          from(String.format("imaps://%s?password=%s&username=%s&consumer.delay=%s&delete=false&fetchSize=1",
              email.url, URLEncoder.encode(email.pwd, "UTF-8"), URLEncoder.encode(email.user, "UTF-8"),
              email.delay)).id(email.id).
            routeId(email.id).
            choice().
              when(anyTruePredicateSet).
                log("accepted email from: $simple{in.header.From}").
                setHeader(ENDPOINT_ID_HEADER, constant(email.id)).
                process(pricehookTaggerProcessor).id("pricehookTagger"). /**/
                split(splitEmailExpr).
                process(emailAttachmentProcessor).
                to("direct:packed").endChoice().
              otherwise().
                log("rejected email from: $simple{in.header.From}").
                to("direct:rejected");
          log.info("Email endpoint is added with id="+email.id);
        }
      }
      from("direct:rejected").
          routeId("REJECTED_EMAILS").
          log(LoggingLevel.INFO, "Rejected email from: ${in.header.From} with subject: ${in.header.Subject}").
          to("log:REJECT_MAILS?level=INFO&showAll=true");

    } catch (Exception e) {
      log.error("Cannot build route", e);
      throw new RuntimeException("Cannot continue", e);
    }
  }
}


