package com.gumirov.shamil.partsib.routefactories;


import org.apache.camel.builder.RouteBuilder;

/**
 * Base interface for HTTP sites processors route factories.
 */
public interface RouteFactory {
  void setStartSubroute(String startSubroute);

  void setEndSubrouteSuccess(String endSubrouteSuccess);

  RouteBuilder createRouteBuilder();

  void setUrl(String url);

  void setUser(String user);

  void setPasswd(String pwd);
}
