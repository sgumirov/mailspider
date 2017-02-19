package com.gumirov.shamil.partsib.configuration.endpoints;

public class Endpoint{
  public String id,user,pwd,url;

  @Override
  public String toString() {
    return "Endpoint{" +
        "id='" + id + '\'' +
        ", user='" + user + '\'' +
        ", url='" + url + '\'' +
        '}';
  }
}
