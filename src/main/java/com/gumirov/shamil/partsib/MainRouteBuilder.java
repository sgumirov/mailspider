package com.gumirov.shamil.partsib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.mail.MailBindingFixNestedAttachments;
import com.gumirov.shamil.partsib.routefactories.RouteFactory;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.plugins.PluginsLoader;
import com.gumirov.shamil.partsib.processors.*;
import com.gumirov.shamil.partsib.util.*;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mail.MailEndpoint;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

/**
 * A Camel Java DSL Router
 */
public class MainRouteBuilder extends RouteBuilder {

  public static final String COMPRESSED_TYPE_HEADER_NAME = "compressor.type";
  public static final String ENDPOINT_ID_HEADER = "endpoint.id";
  public static final String PRICEHOOK_ID_HEADER = "pricehook.id";
  public static final String PRICEHOOK_RULE = "pricehook.rule";
  public static final String CHARSET = "UTF-8";
  public static final String PRICEHOOK_TAGGING_RULES_HEADER = "com.gumirov.shamil.partsib.PRICEHOOK_TAGGING_HEADER";
  public static final String PLUGINS_STATUS_OK = "MAILSPIDER_PLUGINS_STATUS";
  public static final long HOUR_MILLIS = 1000 * 60 * 60; //1 hour in millis
  public static final long DAY_MILLIS = HOUR_MILLIS * 24; //1 day in millis
  public static final SimpleDateFormat mailDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

  //default value
  public static String VERSION = "1.9";

  /**
   * ID for tracking email in logs
   */
  public static final String MID = "MID";
  public static int MAX_UPLOAD_SIZE;
  private int cachedStringHash;
  private int deletedMailCount;
  private long deleteOldMailCheckPeriod;
  private boolean deleteOldMailEnabled;
  private int deleteOldMailAfterDays;
  /**
   * If true pipeline passes whenever PLUGINS_STATUS_OK is set to false.
   */
  private boolean passAnyPluginStatus;

  public enum CompressorType {
    GZIP, ZIP, RAR, _7Z
  }

  //@inject
  public ConfiguratorFactory confFactory = new ConfiguratorFactory();
  public Configurator config = confFactory.getConfigurator();

  /**
   * Used to throw exception for new mail to skip and to pass by old mails in order to delete.
   */
  private DeleteOldMailProcessor skipNewMailProcessor;

  public MainRouteBuilder() {
    int i = 0;
  }

  public MainRouteBuilder(Configurator config) {
    this.config = config;
  }

  public MainRouteBuilder(CamelContext context) {
    super(context);
  }

  public MainRouteBuilder(CamelContext context, Configurator config) {
    super(context);
    this.config = config;
    loadVersion();
  }

  private void loadVersion() {
    Properties ver = new Properties();
    try {
      ver.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
      VERSION = ver.getProperty("version", VERSION);
    } catch (Exception e) {
      log.error("Cannot load version.properties", e);
    }
  }

  public Endpoints getEndpoints() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("endpoints.config.filename") ), CHARSET);
    return mapper.readValue(json, Endpoints.class);
  }

  public ArrayList<EmailAcceptRule> getEmailAcceptRules() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("email.accept.rules.config.filename") ), CHARSET);
    return mapper.readValue(json, new TypeReference<List<EmailAcceptRule>>(){});
  }

  public List<PricehookIdTaggingRule> getPricehookConfig() throws IOException {
    String json = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.get("pricehook.tagging.config.filename")), CHARSET);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, new TypeReference<List<PricehookIdTaggingRule>>(){});
  }

  public int getDeletedMailCount() {
    return skipNewMailProcessor == null ? 0 : skipNewMailProcessor.getPassedBy();
  }

  public List<PricehookIdTaggingRule> loadPricehookConfig(String url) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpGet req = new HttpGet(url);
    CloseableHttpResponse res = null;
    try {
      res = httpclient.execute(req);
      HttpEntity entity = res.getEntity();
      String json = IOUtils.toString(entity.getContent());
      EntityUtils.consume(entity);
      if (json.hashCode() != cachedStringHash) {
        cachedStringHash = json.hashCode();
        log.info("-=| Loaded pricehooks tags config has changed |=- The new one:\n{}", json);
      }
      return parseTaggingRules(json);
    } catch (Exception e){
      return null;
    } finally {
      if (res != null) res.close();
      httpclient.close();
    }
  }

  public static List<PricehookIdTaggingRule> parseTaggingRules(String json) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, new TypeReference<List<PricehookIdTaggingRule>>(){});
  }

  public int getMaxUploadSize(String maxSizeText) {
    return Integer.parseInt(maxSizeText);
  }

  public void configure() {
    log.info("============ MailSpider version: "+VERSION);
    try {
      //debug, will be overriden by config's 'tracing' boolean value
      if (config.is("tracing", false)) {
        log.info("Warning: enabled Camel TRACING");
        getContext().setTracing(true);
      } else
        getContext().setTracing(false);
      FileNameExcluder officeZipFormatsExcluder = filename -> filename != null && (
          filename.endsWith("xlsx") || filename.endsWith("xls") || filename.endsWith("xlsm") || filename.endsWith("xlsb")
          || filename.endsWith("docx")
      );
      //TODO add AT for this:
      passAnyPluginStatus = config.is("plugin.pass.when.error", false);
      Properties notificationConfig = loadNotificationConfig(config.get("notification.config"));
      String notificationUrl = notificationConfig.getProperty("email.uri");
      log.info("Notifications url: "+notificationUrl);
      log.info("Output url: "+getOutputUrl());
      NotificationProcessor notificationProcessor = new NotificationProcessor(notificationConfig);
      ArchiveTypeDetectorProcessor comprDetect = new ArchiveTypeDetectorProcessor(officeZipFormatsExcluder);
      OutputProcessor outputProcessorEndpoint = new OutputProcessor(getOutputUrl());
      PluginsProcessor pluginsProcessor = new PluginsProcessor(getPlugins());
      EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor();
      List<PricehookIdTaggingRule> pricehookRules = getPricehookConfig();
      PricehookTaggerProcessor pricehookIdTaggerProcessor = new PricehookTaggerProcessor(pricehookRules);
      PricehookIdTaggingRulesLoaderProcessor pricehookRulesConfigLoaderProcessor = 
          new PricehookIdTaggingRulesLoaderProcessor(config.get("pricehook.config.url"), getConfigLoaderProvider());
      AttachmentTaggerProcessor attachmentTaggerProcessor = new AttachmentTaggerProcessor();
      SplitAttachmentsExpression splitEmailExpr = new SplitAttachmentsExpression();

      List<String> extensionAcceptList = getExtensionsAcceptList();
      
      MAX_UPLOAD_SIZE = getMaxUploadSize(config.get("max.upload.size", "1024000"));

      //delete old mail config read
      deleteOldMailEnabled = config.is("delete_old_mail.enabled", false);
      deleteOldMailAfterDays = Integer.parseInt(config.get("delete_old_mail.keep.days", "30"));
      deleteOldMailCheckPeriod = HOUR_MILLIS * Integer.parseInt(config.get("delete_old_mail.check_period.hours", "24"));
      if (deleteOldMailCheckPeriod == 0) {
        log.warn("WARNING: Incorrect value for config property: delete_old_mail.check_period.hours=0, fixing that (1 sec now)");
        deleteOldMailCheckPeriod = 1000;
      }
      if (deleteOldMailEnabled) log.info("Delete old mail is: ENABLED (keep days="+deleteOldMailAfterDays+" check period: "+deleteOldMailCheckPeriod+")");
      else log.info("Delete old mail is: DISABLED");

      //idempotent repo config
      FileNameIdempotentRepoManager repoMan = new FileNameIdempotentRepoManager(
          config.get("work.dir", "/tmp")+ File.separatorChar+config.get("idempotent.repo", "idempotent_repo.dat"));
      Endpoints endpoints = getEndpoints();

      //define onException on the top
      onException(SkipMessageException.class).process(new SkipMessageExceptionErrorHandler()).id("myerrorhandler").
          handled(true).
          stop();

      getContext().getStreamCachingStrategy().setSpoolUsedHeapMemoryThreshold(75);
      getContext().getStreamCachingStrategy().setSpoolThreshold(1024000); //1M

      //FTP <production>
      if (config.is("ftp.enabled")) {
        log.info(format("[FTP] Setting up %d source endpoints", endpoints.ftp.size()));
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
        log.info(format("[HTTP] Setting up %d source endpoints", endpoints.http.size()));
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
          filter(exchange -> {
            String name = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            if (null == name) return false;
            name = name.toLowerCase().trim();
            for (String ext : extensionAcceptList) {
              if (name.endsWith(ext.trim())) {
                return true;
              }
            }
            log.warn("Rejected filename by wrong extension: "+name);
            return false;
          }).id("FileExtensionFilter").
          process(pluginsProcessor).id("PluginsProcessor").
          filter((exchange) -> Boolean.TRUE.equals(passAnyPluginStatus || exchange.getIn().getHeader(PLUGINS_STATUS_OK, Boolean.class)) ).id("PluginsStatusFilter").
          to("direct:output").end();

      //log dead letter channel:
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
        // ===== prepare email accept rules
        final List<Predicate> predicatesAnyTrue = new ArrayList<>();
        ArrayList<EmailAcceptRule> rules = getEmailAcceptRules();
        for (EmailAcceptRule rule : rules){
          if (Configurator.isTrue(rule.ignorecase)) {
            predicatesAnyTrue.add(exchange -> exchange.getIn().getHeader(rule.header, String.class) != null &&
                exchange.getIn().getHeader(rule.header, String.class).toUpperCase().contains(rule.contains.toUpperCase()));
          } else {
            predicatesAnyTrue.add(SimpleBuilder.simple("${in.header." + rule.header + "} contains '" + rule.contains + "'"));
          }
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
        //===== END prepare accept rules

        log.info(format("[EMAIL] Setting up %d source endpoints", endpoints.email.size()));
        System.setProperty("mail.mime.decodetext.strict", "false");

        for (Endpoint email : endpoints.email) {
          String url = format(
                  "%s?password=%s" +
                  "&username=%s" +
                  "&consumer.delay=%s" +
                  "&consumer.useFixedDelay=true" +
                  "&consumer.initialDelay=300" +
                  "&unseen=true" +
                  "&peek=true" +
                  "&fetchSize=25" +
                  "&skipFailedMessage=true" +
                  "&maxMessagesPerPoll=25"+
                  "&mail.imap.ignorebodystructuresize=true"+
                  "&mail.imap.partialfetch=false"+
                  "&mail.imaps.partialfetch=false"+
                  "&mail.debug=true"+
                  "%s",
              addProtocolPrefix(email.url),
              URLEncoder.encode(email.pwd, "UTF-8"),
              URLEncoder.encode(email.user, "UTF-8"),
              email.delay, Util.formatParameters(email.parameters));

          //set ours MailBinding implementation
          log.info("Email endpoint URL: "+Util.removeSensitiveData(url, "password"));
          MailEndpoint mailEndpoint = getContext().getEndpoint(url, MailEndpoint.class);
          mailEndpoint.setBinding(new MailBindingFixNestedAttachments());

          if (deleteOldMailEnabled)
            createDeleteOldMailPath(email, deleteOldMailAfterDays);

          EndpointSpecificUrl eurl = new EndpointSpecificUrl(email);

          from(mailEndpoint).id("server.source-"+email.id).routeId("source-"+email.id).
              to(eurl.apply("direct:emailreceived"));

          from(eurl.apply("direct:emailreceived")).routeId("received-"+email.id).
            multicast().parallelProcessing().
              to(
                  eurl.apply("direct:processemail"),
                  eurl.apply("direct:notification")
              );

          from(eurl.apply("direct:notification")).routeId("notification-"+email.id).
            process(notificationProcessor).id("notification-processor-"+email.id).
            choice().
              when(exchange -> ((boolean) exchange.getIn().getHeader(NotificationProcessor.SKIP_NOTIFICATION, false))).
                stop().endChoice().
            end().
            log("Sending notification").id("notification-sender-log-"+email.id).
            to(notificationUrl).id("notification-sender-"+email.id);

          from(eurl.apply("direct:processemail")).
            process(exchange -> {
              String s;
              if (null != (s = exchange.getIn().getHeader("Subject", String.class)))
                exchange.getIn().setHeader("Subject", MimeUtility.decodeText(s.trim()).replaceAll(" +", " "));
              if (null != (s = exchange.getIn().getHeader("From", String.class)))
                exchange.getIn().setHeader("From", MimeUtility.decodeText(s.trim()).replaceAll(" +", " "));
            }).id("HeadersMimeDecoder-"+email.id).
            choice().
              when(emailAcceptPredicate).
                log(LoggingLevel.INFO, "Accepted (primary) email from: '$simple{in.header.From}' with Subject: '$simple{in.header.Subject}' sent at: '$simple{in.header.Date}'").
                setHeader(ENDPOINT_ID_HEADER, constant(email.id)).
                to("direct:acceptedmail").
                endChoice().
              otherwise().
                log(LoggingLevel.INFO, "Rejected email as not matched any of accepted rules: from=$simple{in.header.From}").
                to("direct:rejected");
          log.info("Email endpoint is added with id="+email.id);
        }

        //pricehook tagging and attachment extraction
        from("direct:acceptedmail").routeId("acceptedmail").
            process(exchange -> {exchange.getIn().setHeader(MID, Util.getMID(exchange.getIn()));}).
            log(LoggingLevel.INFO, "[${in.header.MID}] Accepted email sent at ${in.header.Date} from ${in.header.From} with subject '${in.header.Subject}'").
            streamCaching().
            process(pricehookRulesConfigLoaderProcessor).id("pricehookConfigLoader").
            process(pricehookIdTaggerProcessor).id(PricehookTaggerProcessor.ID).
            choice().
              when(exchange -> null == exchange.getIn().getHeader(PRICEHOOK_ID_HEADER)).
                log(LoggingLevel.INFO, "[${in.header.MID}] Rejecting email due to no tag: sent at ${in.header.Date} from ${in.header.From}").
                to("direct:rejected").
              endChoice().
            otherwise().
            log(LoggingLevel.INFO, "[${in.header.MID}] Starting to process attachments for email: sent at ${in.header.Date} from ${in.header.From}").
            split(splitEmailExpr).
            process(emailAttachmentProcessor).
            process(exchange -> {
              //logging only here
              Message in = exchange.getIn();
              log.info("["+exchange.getIn().getHeader(MID)+"]"+" Attachments size before split: "+in.getAttachments().size());
              for (String s : in.getAttachmentNames()) {
                log.info("["+exchange.getIn().getHeader(MID)+"]"+" Attachment=" + s);
              }
            }).
            process(attachmentTaggerProcessor).id(AttachmentTaggerProcessor.ID).
            process(exchange ->
                log.info("Attachment: name={} tag={}",
                exchange.getIn().getHeader(Exchange.FILE_NAME),
                exchange.getIn().getHeader(PRICEHOOK_ID_HEADER))).
            id("taglogger").
            to("direct:packed");

        from("direct:rejected").
            routeId("REJECTED_EMAILS").
            filter(exchange -> false).
            to("log:REJECTED");
      }
    } catch (Exception e) {
      log.error("Cannot build route", e);
      throw new RuntimeException("Cannot continue", e);
    }
  }

  public String getOutputUrl() {
    return config.get("output.url");
  }

  private void createDeleteOldMailPath(Endpoint email, int daysKeep) throws UnsupportedEncodingException {
    long mailKeepHours = daysKeep*24;
    String url = format(
            "%s?password=%s&username=%s" +
            "&consumer.delay=%s" +
//            "&consumer.useFixedDelay=true" +
            "&consumer.initialDelay=1" +
            "&delete=true" +
            "&searchTerm.toSentDate=now-%sh" +
            "&searchTerm.unseen=false"+
            "&unseen=false" +
            "&peek=true" +
            "&fetchSize=250" +
            "&skipFailedMessage=true" +
            "&maxMessagesPerPoll=250"+
            "&mail.imap.ignorebodystructuresize=true"+
            "&mail.imap.partialfetch=false"+
            "&mail.imaps.partialfetch=false"+
            "&mail.debug=true"
            +"%s"
        ,
        addProtocolPrefix(email.url),
        URLEncoder.encode(email.pwd, "UTF-8"),
        URLEncoder.encode(email.user, "UTF-8"),
        deleteOldMailCheckPeriod,
        mailKeepHours, Util.formatParameters(email.parameters)
        );

    log.info("delete mail URL="+Util.removeSensitiveData(url, "password"));

    skipNewMailProcessor = new DeleteOldMailProcessor(daysKeep);
    from(url).routeId("delete-old-mail-route").id("delete-mail-source").
        process(skipNewMailProcessor). //should throw exception to skip mail.
        id("delete-mail-processor").
        to("log:DELETE_MAIL");

  }

  public Properties loadNotificationConfig(String fname) throws IOException {
    Properties p = new Properties();
    p.load(getClass().getClassLoader().getResourceAsStream(fname));
    return p;
  }

  /**
   * Override to use own configloader (for unit-tests).
   * @return
   */
  public PricehookIdTaggingRulesConfigLoaderProvider getConfigLoaderProvider() {
    return url -> MainRouteBuilder.this.loadPricehookConfig(url);
  }

  /**
   * @return lower-case list of extensions
   */
  public List<String> getExtensionsAcceptList() {
    return Arrays.asList(config.get("file.extension.accept.list", "xls,txt,csv,xlsx").toLowerCase().split(","));
  }

  private String addProtocolPrefix(String url) {
    if (!url.contains("://")) {
      return config.get("default.email.protocol", "imaps")+"://"+url;
    }
    return url;
  }

  public List<Plugin> getPlugins() {
    return new PluginsLoader(config.get("plugins.config.filename")).getPlugins();
  }
}


