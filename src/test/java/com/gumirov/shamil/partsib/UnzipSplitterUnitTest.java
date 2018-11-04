package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.processors.UnpackerSplitter;
import com.gumirov.shamil.partsib.util.SevenZipStreamUnpacker;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

public class UnzipSplitterUnitTest extends CamelTestSupport {

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:from")
  protected FluentProducerTemplate template;
  
  private final byte[] contents = prepareContents();;

  @Test
  public void testSendMatchingMessage() throws Exception {
    resultEndpoint.expectedMessageCount(2);
    resultEndpoint.whenAnyExchangeReceived(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        String fname = (String) exchange.getIn().getHeader(Exchange.FILE_NAME);
        log.info("Received: "+fname);
        byte[] b = Util.readFully((InputStream) exchange.getIn().getBody());
        if (!Arrays.equals(b, contents)) {
          assertEquals("body length must be same", contents.length, b.length);
          throw new RuntimeException("Wrong body for name="+
              fname);
        }
      }
    });
    resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, "f1.txt", "dir"+File.separatorChar+"f2.txt");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(bos);
    zos.putNextEntry(new ZipEntry("f1.txt"));
    zos.write(contents);
    zos.flush();
    zos.closeEntry();
    zos.putNextEntry(new ZipEntry("dir/f2.txt"));
    zos.write(contents);
    zos.closeEntry();
    zos.flush();
    zos.close();
    
    bos.close();
    byte[] zip = bos.toByteArray();
    System.out.println("zip.length="+zip.length);

    ByteArrayInputStream bis = new ByteArrayInputStream(zip);
    
    Exchange exchange = template.to("direct:from").withBody(bis).
        withHeader("CamelFileName", "archive.zip").
        withHeader("CamelFileLength", zip.length).
        send();
    assertTrue("Exchange must not fail", !exchange.isFailed());

    resultEndpoint.assertIsSatisfied();
  }

  private byte[] prepareContents() {
    byte[] b = new byte[102400];
    for (int i = 0; i < b.length; i+=1024){
      b[i] = 1;
    }
    b[b.length-1] = 1; //Fix for a ZipOutputStream bug forgetting trailing zeroes
    return b;
  }

  @Before
  public void setup() {
    File idempotentRepo = new File("target/idempotent_repo.dat");
    if (idempotentRepo.exists()) {
      idempotentRepo.delete();
      log.info("removed repo");
    }
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder()
    {
      @Override
      public void configure() {
        from("direct:from").
            split(beanExpression(new UnpackerSplitter(new SevenZipStreamUnpacker()), "unpack")).
            to("mock:result");
      }
    };
  }
}
