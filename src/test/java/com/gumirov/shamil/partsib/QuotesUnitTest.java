package com.gumirov.shamil.partsib;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 28/5/2017 Time: 18:41<br/>
 */
public class QuotesUnitTest extends CamelTestSupport {
  private static final String HEADER = "HEADER_TEST";

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Override
  public boolean isDumpRouteCoverage() {
    return true;
  }

  @Test
  public void testQuotes() throws InterruptedException {
    resultEndpoint.expectedMessageCount(2);
    template.sendBodyAndHeader("anything", HEADER, "something 'with' \"quotes\"");
    template.sendBodyAndHeader("anything", HEADER, "\"");
    resultEndpoint.assertIsSatisfied();
  }

  @Test
  public void testNoQuotes() throws InterruptedException {
    resultEndpoint.expectedMessageCount(0);
    template.sendBodyAndHeader("anything", HEADER, "something 'quotes'");
    template.sendBodyAndHeader("anything", HEADER, "");
    resultEndpoint.assertIsSatisfied();
  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {
        Predicate predicate = SimpleBuilder.simple("${in.header."+HEADER+"} contains '\"'");
        from("direct:start").process(exchange -> {
          if (!predicate.matches(exchange)) {
            exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
            log.info("Stopping exchange at message: "+exchange.getIn());
          }
        }).to("mock:result");
      }
    };
  }
}
