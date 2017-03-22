package com.gumirov.shamil.partsib;

import org.apache.camel.*;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

/**
 *
 */
public class FilterPredicateRouteTest extends CamelTestSupport {
  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  private final String body = "file text contents";
  private final byte[] contents = body.getBytes();
  
  private List<Predicate> predicatesAnyTrue;
  
  Predicate anyTruePredicateSet = new Predicate() {
    @Override
    public boolean matches(Exchange exchange) {
      for (Predicate p : predicatesAnyTrue){
        if (p.matches(exchange)) {
          return true;
        }
      }
      return false;
    }
  };
  
  @Before
  public void prepare(){
    predicatesAnyTrue = new ArrayList<>();
    predicatesAnyTrue.add(SimpleBuilder.simple("${in.header.From} contains \"good\""));
    predicatesAnyTrue.add(SimpleBuilder.simple("${in.header.From} contains \"best\""));
    predicatesAnyTrue.add(SimpleBuilder.simple("${in.header.Subject} contains \"cool\""));
  }

  @Test
  public void testSendMatchingMessage() throws Exception {

    resultEndpoint.expectedMessageCount(4);
    template.sendBodyAndHeader("", "From", "good@mail.ru");
    template.sendBodyAndHeader("", "From", "all@best.ru");
    template.sendBodyAndHeader("", "From", "all@mail.ru");
    template.sendBodyAndHeader("", "Subject", "cool letter");
    HashMap<String,Object> headers = new HashMap<>();
    headers.put("From", "some.shitty@gmail.com");
    headers.put("Subject", "cool letter!");
    template.sendBodyAndHeaders("", headers);
    headers.put("Subject", "increase your PC");
    resultEndpoint.assertIsSatisfied();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start").
            filter(anyTruePredicateSet).
            log("Went on: $simple{in.header.From}").
            to("mock:result");
      }
    };
  }
}
