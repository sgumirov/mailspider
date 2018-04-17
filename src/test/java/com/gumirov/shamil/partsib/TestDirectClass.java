package com.gumirov.shamil.partsib;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class TestDirectClass extends CamelTestSupport {
  @Produce(uri = "direct:start")
  protected ProducerTemplate template;
  @EndpointInject(uri = "mock:to")
  protected MockEndpoint to;

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start").log(LoggingLevel.INFO, "message").to("mock:to");
      }
    };
  }

  @Test
  public void testConnectionError() throws Exception {
    getMockEndpoint("mock:to").expectedMessageCount(1);

    MockEndpoint mock = getMockEndpoint("mock:to");
    mock.whenAnyExchangeReceived(new Processor() {
      public void process(Exchange exchange) throws Exception {
//        throw new RuntimeException();
      }
    });

    template.sendBody("Testing");
    assertMockEndpointsSatisfied();
  }
}
