package com.gumirov.shamil.partsib.configuration.endpoints;

public class Endpoint{
  public String id,user,pwd,url;
  public String delay = "600000"; //10 mins
  /**
   * FQCN implementing com.gumirov.shamil.partsib.factories.RouteFactory.
   */
  public String factory;
  
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
