package com.gumirov.shamil.partsib.configuration.endpoints;

import java.util.Map;

public class Endpoint{
  public String id,user,pwd,url;
  public String delay = "600000"; //10 mins
  /**
   * FQCN implementing com.gumirov.shamil.partsib.factories.RouteFactory.
   */
  public String factory;
  public Map<String, String> parameters;

  @Override
  public String toString() {
    return "Endpoint{" +
        "id='" + id + '\'' +
        ", user='" + user + '\'' +
        ", url='" + url + '\'' +
        ", delay='" + delay + '\'' +
        '}';
  }
}
