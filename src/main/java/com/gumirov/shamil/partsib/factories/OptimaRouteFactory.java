package com.gumirov.shamil.partsib.factories;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpConstants;
import org.apache.camel.component.http4.HttpEndpoint;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.*;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenix on 04.03.2017.
 */
public class OptimaRouteFactory implements RouteFactory {

//  private final String url = "http4://im.mad.gd/1.php?r=site/products"; //not logged in -> redirect to log in
//  private final String url = "http4://im.mad.gd/2.php?r=site%2Fproducts";
  private final String url = "https4://optma.ru/index.php?r=site/products";
  private final String loginUrl = "https://optma.ru/index.php?r=site/login";
//  private final String loginUrl = "http://im.mad.gd/login.php?r=site/products";

  private String startSubroute, endSubrouteSuccess;
  private String user;
  private String pwd;

  @Override
  public void setUrl(String url) {
    //this.url = url;
    //todo
  }

  @Override
  public void setUser(String user) {
    this.user = user;
  }

  @Override
  public void setPasswd(String pwd) {
    this.pwd = pwd;
  }

  @Override
  public void setStartSubroute(String startSubroute) {
    this.startSubroute = startSubroute;
  }

  @Override
  public void setEndSubrouteSuccess(String endSubrouteSuccess) {
    this.endSubrouteSuccess = endSubrouteSuccess;
  }

  HttpResponseInterceptor contentEncodingFixerInterceptor = new HttpResponseInterceptor()  {
    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
      Header contentEncodingHeader = response.getFirstHeader(HTTP.CONTENT_ENCODING);
      if(contentEncodingHeader != null && ! (
          "gzip".equals(contentEncodingHeader.getValue()) ||
              "compress".equals(contentEncodingHeader.getValue()) ||
              "deflate".equals(contentEncodingHeader.getValue()) ||
              "identity".equals(contentEncodingHeader.getValue()) ||
              "br".equals(contentEncodingHeader.getValue()) )
          ) {
        response.removeHeaders(HTTP.CONTENT_ENCODING);
        response.addHeader(HTTP.CONTENT_ENCODING, "identity");
      }
    }
  };

  @Override
  public RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        OptimaAuthProcessor authorizer = new OptimaAuthProcessor(user, pwd, loginUrl);
        errorHandler(deadLetterChannel("direct:deadletter").logExhaustedMessageBody(true));
//        HttpComponent http4 = getContext().getComponent("https4", HttpComponent.class);
//        http4.setHttpClientConfigurer(configurer);
        HttpEndpoint httpEndpoint = (HttpEndpoint) getContext().getEndpoint(url);
        HttpProcessor httpProcessor = getHttpProcessor();
        
        HttpClient client = HttpClients.custom().setHttpProcessor(httpProcessor).build();
        httpEndpoint.setHttpClient(client);
        
        from(startSubroute).
            //see if authorized?
            setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST)).
            setHeader(Exchange.CONTENT_TYPE, constant(HttpConstants.CONTENT_TYPE_WWW_FORM_URLENCODED)).
            setBody(simple("dwn=1")).
            to(httpEndpoint).
            choice().
            //not auth? authorize with OptimaAuthProcessor
            when(header(Exchange.CONTENT_TYPE).contains("text/html")). //not authorized
            filter(header(OptimaAuthProcessor.AUTH_HEADER).isNull()). //do not authorize twice. todo do we need error/logging in this case?
            process(authorizer).id("OptimaAuthorizer").
            to(startSubroute).
            endChoice().
            //authorized & the file is in response
            when(header(Exchange.CONTENT_TYPE).contains("application/octet-stream")).
            setHeader(Exchange.FILE_NAME, constant("pricelist_2017-03-03_18-37.csv")). //todo get this from headers disposition
            //setHeader(Exchange.FILE_LENGTH, header(Exchange.CONTENT_LENGTH)).
            process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception {
                byte[] b = exchange.getContext().getTypeConverter().convertTo(byte[].class, exchange.getIn().getBody(InputStream.class));
                exchange.getIn().setHeader(Exchange.FILE_LENGTH, b.length);
                exchange.getIn().setBody(b);
                log.info("OptimaRouteFactory$processor: saved file length="+b.length+" from http");
              }
            }).
            log(LoggingLevel.DEBUG, org.slf4j.LoggerFactory.getLogger(OptimaRouteFactory.class.getSimpleName()), "Got body: ${body}").
            to(endSubrouteSuccess);
      }
    };
  }

  private HttpProcessor getHttpProcessor() {
    BasicHttpProcessor hp = new BasicHttpProcessor();

    hp.addInterceptor(contentEncodingFixerInterceptor);

    // Required protocol interceptors
    hp.addInterceptor(new RequestContent());
    hp.addInterceptor(new RequestTargetHost());
    hp.addInterceptor(new RequestDefaultHeaders());

    // Recommended protocol interceptors
    hp.addInterceptor(new RequestConnControl());
    hp.addInterceptor(new RequestUserAgent());
    hp.addInterceptor(new RequestExpectContinue());

    // HTTP state management interceptors
    hp.addInterceptor(new RequestAddCookies());
    hp.addInterceptor(new ResponseProcessCookies());

    // HTTP authentication interceptors
    hp.addInterceptor(new RequestTargetAuthentication());
    hp.addInterceptor(new RequestProxyAuthentication());

    return hp;
  }
}

class OptimaAuthProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(OptimaAuthProcessor.class);

  public static final String AUTH_HEADER = "OPTIMA-AUTHORIZED";
  private String user;
  private String pwd;
  private String url;

  public OptimaAuthProcessor(String user, String pwd, String url) {
    this.user = user;
    this.pwd = pwd;
    this.url = url;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    //todo
    logger.info("Logging attempt");
    exchange.getIn().setHeader(AUTH_HEADER, "yep");

    List<String> cookies = doAuth();
    //for (String c : cookies)
    if (cookies.size() > 0)
    {
      exchange.getIn().setHeader("Cookie", cookies.get(cookies.size()-1));
    }
  }

  private List<String> doAuth() throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    try {
      HttpPost httppost = new HttpPost(url);

      httppost.setEntity(new StringEntity("LoginForm%5Busername%5D="+URLEncoder.encode(user, "UTF-8")
          +"&LoginForm%5Bpassword%5D="+URLEncoder.encode(pwd, "UTF-8")+"&LoginForm%5BrememberMe%5D=0&ytf0=&timezoneoffset=-420",
         ContentType.APPLICATION_FORM_URLENCODED));

      System.out.println("Executing request: " + httppost.getRequestLine());
      CloseableHttpResponse response = httpclient.execute(httppost);
      try {
        System.out.println("-------------------auth repsponse---------------------");
        for (Header h : response.getAllHeaders()) {
          System.out.println(h.getName()+": "+h.getValue());
        }
        System.out.println(response.getStatusLine());
        System.out.println(EntityUtils.toString(response.getEntity()));
        ArrayList<String> list = new ArrayList<>();
        Header[] c = response.getHeaders("Set-Cookie");
        for (Header h : c){
          list.add(h.getValue());
          logger.info("Cookie: "+h.getValue());
        }
        return list;
      } finally {
        response.close();
      }
    } catch (IOException e) {
      logger.info("[OptimaAuthProcessor] error", e);
      return new ArrayList<>();
    } finally {
      httpclient.close();
    }
  }
}
