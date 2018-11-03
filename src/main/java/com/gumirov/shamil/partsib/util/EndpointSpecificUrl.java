package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class EndpointSpecificUrl {
  Endpoint endpoint;

  public EndpointSpecificUrl(Endpoint endpoint) {
    this.endpoint = endpoint;
  }

  public String apply(String directUrl) {
    return apply(directUrl, endpoint);
  }

  public static String apply(String uri, Endpoint endpoint) {
    return uri + "-" + endpoint.id;
  }
}
