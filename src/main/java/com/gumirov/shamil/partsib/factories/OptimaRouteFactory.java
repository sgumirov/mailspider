package com.gumirov.shamil.partsib.factories;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpConstants;

/**
 * Created by phoenix on 04.03.2017.
 */
public class OptimaRouteFactory implements RouteFactory {

  private final String url = "https4://im.mad.gd/1.php?r=site/products";
//  private final String url = "https4://optma.ru/index.php?r=site/products";
  private final String loginUrl = "https4://optma.ru/index.php?r=site/login";

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

  @Override
  public RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        OptimaAuthProcessor authorizer = new OptimaAuthProcessor(user, pwd);
        from(startSubroute).
            setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST)).
            setHeader(Exchange.CONTENT_TYPE, constant(HttpConstants.CONTENT_TYPE_WWW_FORM_URLENCODED)).
            setBody(simple("dwn=1")).
            to(url).
            choice().
            when(header(Exchange.CONTENT_TYPE).contains("text/html")). //not authorized
            process(authorizer).id("OptimaAuthorizer").
            to(startSubroute).
            endChoice().
            when(header(Exchange.CONTENT_TYPE).contains("application/octet-stream")).
            log(LoggingLevel.ERROR, org.slf4j.LoggerFactory.getLogger(OptimaRouteFactory.class.getSimpleName()), "Got body: ${body}").
            to(endSubrouteSuccess);
      }
    };
  }
}

class OptimaAuthProcessor implements Processor {

  public OptimaAuthProcessor(String user, String pwd) {
    
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    //todo
  }
}