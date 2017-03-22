package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.processors.UnpackerSplitter;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

/**
 *
 */
public class Un7zipSplitterTest extends CamelTestSupport {
  @EndpointInject(uri = "mock:result")
  protected MockEndpoint mockEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;
  
  @Test
  public void testUnzip() throws IOException, InterruptedException {
    File f = new File("src/data/test.full/rarfile.rar");
    mockEndpoint.setExpectedMessageCount(2);
    mockEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, Arrays.asList("rarfile2.txt", "rartxt.txt"));
    byte[] b = new byte[(int) f.length()];
    Util.readFully(new FileInputStream(f), b);
    template.sendBodyAndHeader(b, Exchange.FILE_NAME, "src/data/rarfile.rar");
    mockEndpoint.assertIsSatisfied();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start").
            split(beanExpression(new UnpackerSplitter(), "unpack")).
            log("Unpacked: ${in.header.CamelFileName}").
            to("mock:result");
      }
    };
  }
}
