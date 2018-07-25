package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.processors.ArchiveTypeDetectorProcessor;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class ArchiveTypeDetectorProcessorUnitTest extends CamelTestSupport {
  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  final byte[] zip = {0x50, 0x4B, 0x03, 0x04};
  final byte[] rar = {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00};
  final byte[] gzip = {0x1f, (byte)(0x8b)};
  final byte[] _7z = {0x37, 0x7a, (byte) (0xbc), (byte) (0xaf), 0x27, 0x1c};

  private final String folder = "archives";
  private final String zipFile =  folder+ File.separatorChar+"a.zip";
  private final String gzipFile = folder+ File.separatorChar+"a.gz";
  private final String rarFile =  folder+ File.separatorChar+"a.rar";
  private final String _7zFile =  folder+ File.separatorChar+"a.7z";

  @Test
  public void testBytes() throws InterruptedException {
    test(gzip, MainRouteBuilder.CompressorType.GZIP);
    test(zip, MainRouteBuilder.CompressorType.ZIP);
    test(rar, MainRouteBuilder.CompressorType.RAR);
    test(_7z, MainRouteBuilder.CompressorType._7Z);
  }

  @Test
  public void testFiles() throws InterruptedException, IOException {
    test(Util.readFully(getClass().getClassLoader().getResource(gzipFile).openStream()), MainRouteBuilder.CompressorType.GZIP);
    test(Util.readFully(getClass().getClassLoader().getResource(zipFile).openStream()), MainRouteBuilder.CompressorType.ZIP);
    test(Util.readFully(getClass().getClassLoader().getResource(rarFile).openStream()), MainRouteBuilder.CompressorType.RAR);
    test(Util.readFully(getClass().getClassLoader().getResource(_7zFile).openStream()), MainRouteBuilder.CompressorType._7Z);
  }

  @Test
  //todo: check that archiveTypeProcessor handles input streams that does not support mark (caches correctly)
  public void testNoMarkInputStream() throws IOException, InterruptedException {
    final byte[] expected = Util.readFully(getClass().getClassLoader().getResource(gzipFile).openStream());
    InputStream ris = getClass().getClassLoader().getResource(gzipFile).openStream();
    InputStream is = new InputStream() {
      @Override
      public int read() throws IOException {
        return ris.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return ris.read(b, off, len);
      }

      @Override
      public void close() throws IOException {
        ris.close();
      }
    };
    resultEndpoint.expectedMessageCount(1);
    resultEndpoint.expectedHeaderReceived(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.GZIP);
    resultEndpoint.expectedMessagesMatches(exchange -> {
      InputStream in = (InputStream) exchange.getIn().getBody();
      try {
        byte[] b = Util.readFully(in);
        log.info("Assert: read "+b.length+" bytes");
        return Arrays.equals(b, expected);
      } catch (IOException e) {
        log.error("Cannot read body", e);
        return false;
      }
    });
    template.sendBodyAndHeader(is, Exchange.FILE_NAME, "a.gz");
    resultEndpoint.assertIsSatisfied();
    resultEndpoint.reset();
  }

  public void test(Object body, MainRouteBuilder.CompressorType expectedType) throws InterruptedException {
    resultEndpoint.expectedMessageCount(1);
    template.sendBodyAndHeader(body, Exchange.FILE_NAME, expectedType.toString());
    resultEndpoint.expectedHeaderReceived(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, expectedType);
    resultEndpoint.assertIsSatisfied();
    resultEndpoint.reset();
  }

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() {
        from("direct:start").
            process(new ArchiveTypeDetectorProcessor(null)).
//            log("Went on: $simple{in.header.From}").
            to("mock:result");
      }
    };
  }
}
