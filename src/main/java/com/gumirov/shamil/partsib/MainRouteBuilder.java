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
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.plugins.PluginsLoader;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.FileNameExcluder;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mail.SplitAttachmentsExpression;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

/**
 * A Camel Java DSL Router
 */
public class MainRouteBuilder extends RouteBuilder {
  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ENDPOINT_ID_HEADER = "endpoint.id";
  public static final String PRICEHOOK_ID_HEADER = "pricehook.id";
  public static final String CHARSET = "UTF-8";
  public static final String PRICEHOOK_TAGGING_RULES_HEADER = "com.gumirov.shamil.partsib.PRICEHOOK_TAGGING_HEADER";
  public static int MAX_UPLOAD_SIZE;

  public enum CompressorType {
    GZIP, ZIP, RAR, _7Z
  }

  //@inject
  public ConfiguratorFactory confFactory = new ConfiguratorFactory();
  public Configurator config = confFactory.getConfigurator();

  public MainRouteBuilder() {}

  public MainRouteBuilder(Configurator config) {
    this.config = config;
  }

  public MainRouteBuilder(CamelContext context) {
    super(context);
  }

  public MainRouteBuilder(CamelContext context, Configurator config) {
    super(context);
    this.config = config;
  }
  
  public Endpoints getEndpoints() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("endpoints.config.filename") ), CHARSET);
    return mapper.readValue(json, Endpoints.class);
  }

  public ArrayList<EmailRule> getEmailAcceptRules() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("email.accept.rules.config.filename") ), CHARSET);
    return mapper.readValue(json, new TypeReference<List<EmailRule>>(){});
  }

  public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("pricehook.tagging.config.filename")), CHARSET);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, new TypeReference<List<PricehookIdTaggingRule>>(){});
  }

  public List<PricehookIdTaggingRule> loadPricehookConfig(String url) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpGet req = new HttpGet(url);
    CloseableHttpResponse res = httpclient.execute(req);
    try {
      HttpEntity entity = res.getEntity();
      String json = IOUtils.toString(entity.getContent());
      EntityUtils.consume(entity);
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(json, new TypeReference<List<PricehookIdTaggingRule>>(){});
    } finally {
      res.close();
    }
  }

  public int getMaxUploadSize(String maxSizeText) {
    return Integer.parseInt(maxSizeText);
  }

  public void configure() {
    try {
      //debug
      getContext().setTracing(Boolean.TRUE);
      // lambda-a-a-a
      FileNameExcluder excelExcluder = filename -> filename != null && (
          filename.endsWith("xlsx") || filename.endsWith("xls") || filename.endsWith("xlsm") || filename.endsWith("xlsb")
      );
      ArchiveTypeDetectorProcessor comprDetect = new ArchiveTypeDetectorProcessor(excelExcluder);
      OutputProcessor outputProcessorEndpoint = new OutputProcessor(config.get("output.url"));
      PluginsProcessor pluginsProcessor = new PluginsProcessor(getPlugins());
      EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();
      List<PricehookIdTaggingRule> pricehookRules = getPricehookConfig();
      PricehookTaggerProcessor pricehookIdTaggerProcessor = new PricehookTaggerProcessor(pricehookRules);
      PricehookIdTaggingRulesLoaderProcessor pricehookRulesConfigLoaderProcessor = 
          new PricehookIdTaggingRulesLoaderProcessor(config.get("pricehook.config.url"), url -> MainRouteBuilder.this.loadPricehookConfig(url));
      
      MAX_UPLOAD_SIZE = getMaxUploadSize(config.get("max.upload.size", "1024000"));

      SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();

      FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(
          config.get("work.dir", "/tmp")+ File.separatorChar+config.get("idempotent.repo", "idempotent_repo.dat"));
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

//email protocol
      if (config.is("email.enabled")) {
        //prepare email accept rules
        final List<Predicate> predicatesAnyTrue = new ArrayList<>();
        ArrayList<EmailRule> rules = getEmailAcceptRules();
        for (EmailRule rule : rules){
          predicatesAnyTrue.add(SimpleBuilder.simple("${in.header."+rule.header+"} contains \""+rule.contains+"\""));
          log.info("Email Accept Rule["+rule.id+"]: header="+rule.header+" contains='"+rule.contains+"'");
        }

        final Predicate emailAcceptPredicate = exchange -> {
          for (Predicate p : predicatesAnyTrue){
            if (p.matches(exchange)) {
              return true;
            }
          }
          return false;
        };

        log.info(String.format("[EMAIL] Setting up %d source endpoints", endpoints.email.size()));
        for (Endpoint email : endpoints.email) {
          //fetchSize=1 1 at a time
          from(String.format("imaps://%s?password=%s&username=%s&consumer.delay=%s&consumer.useFixedDelay&" +
                  "delete=false&" +
//                  "sortTerm=reverse,date&" + //todo Fill bug to Camel
                  "unseen=true&" +
                  "peek=true&" +
                  "fetchSize=25&" +
                  "skipFailedMessage=true&" +
                  "maxMessagesPerPoll=25",
              email.url, URLEncoder.encode(email.pwd, "UTF-8"), URLEncoder.encode(email.user, "UTF-8"),
              email.delay)).id(email.id).
            routeId(email.id).
            process(exchange -> exchange.getIn().setHeader("Subject", MimeUtility.decodeText(exchange.getIn().getHeader("Subject", String.class)))).id("SubjectMimeDecoder").
            choice().
              when(emailAcceptPredicate).
                log(LoggingLevel.INFO, "Accepted email from: $simple{in.header.From}").
                setHeader(ENDPOINT_ID_HEADER, constant(email.id)).
                to("direct:acceptedmail").
                endChoice().
              otherwise().
                log("rejected email from: $simple{in.header.From}").
                to("direct:rejected");
          log.info("Email endpoint is added with id="+email.id);
        }
      }

      from("direct:acceptedmail").
          process(pricehookRulesConfigLoaderProcessor).id("pricehookConfigLoader").
          process(pricehookIdTaggerProcessor).id("pricehookTagger").
          filter(exchange -> null != exchange.getIn().getHeader(PRICEHOOK_ID_HEADER)).
          split(splitEmailExpr).
          process(emailAttachmentProcessor).
          to("direct:packed");

      from("direct:rejected").
          routeId("REJECTED_EMAILS").
          log(LoggingLevel.INFO, "Rejected email from: ${in.header.From} with subject: ${in.header.Subject}").
          to("log:REJECT_MAILS?level=INFO&showAll=true");

    } catch (Exception e) {
      log.error("Cannot build route", e);
      throw new RuntimeException("Cannot continue", e);
    }
  }

  public List<Plugin> getPlugins() {
    return new PluginsLoader(config.get("plugins.config.filename")).getPlugins();
  }
}


