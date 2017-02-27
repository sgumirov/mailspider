package com.gumirov.shamil.partsib.configuration.endpoints;

public class Endpoint{
  public String id,user,pwd,url;
  public String delay = "600000"; //10 mins

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
