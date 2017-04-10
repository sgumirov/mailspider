package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.processors.UnpackerSplitter;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.apache.camel.builder.ExpressionBuilder.append;
import static org.apache.camel.builder.ExpressionBuilder.beanExpression;

public class UnzipSplitterUnitTest extends CamelTestSupport {

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:from")
  protected FluentProducerTemplate template;
  
  private String body = "file text contents";
  private byte[] contents = body.getBytes();

  @Test
  public void testSendMatchingMessage() throws Exception {
    contents = prepareContents();

    resultEndpoint.expectedMessageCount(2);
    resultEndpoint.expectedBodiesReceivedInAnyOrder(new Object[]{ new String(contents), new String(contents) });
    resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(Exchange.FILE_NAME, new Object[]{"f1.txt", "dir"+File.separatorChar+"f2.txt"});

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(bos);
    zos.putNextEntry(new ZipEntry("f1.txt"));
    zos.write(contents);
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
    
    template.to("direct:from").withBody(bis).
        withHeader("CamelFileName", "archive.zip").
        withHeader("CamelFileLength", zip.length).
        send();

    resultEndpoint.assertIsSatisfied();
  }

  private byte[] prepareContents() {
    byte[] b = new byte[10240000];
    for (int i = 0; i < b.length; i+=1024){
      b[i] = 1;
    }
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
  protected RoutesBuilder createRouteBuilder() throws Exception
  {
    return new RouteBuilder()
    {
      @Override
      public void configure() throws Exception
      {
        from("direct:from").
//            split(new ZipSplitter()).
            split(beanExpression(new UnpackerSplitter(), "unpack")).
            to("mock:result");
      }
    };
  }
}
