package com.gumirov.shamil.partsib.test;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 17/2/2017 Time: 02:13<br/>
 */
public class MainRouteTest extends CamelTestSupport {

  final String ftpDir = "/tmp/test";
  final String resDir = "src/data/test";

  @Before
  public void setupFTP() throws IOException {
    FileUtils.copyDirectory(new File(resDir), new File(ftpDir));
  }

  @After
  public void cleanFTP() throws IOException {
    FileUtils.deleteDirectory(new File(ftpDir));
  }



  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {

      }
    };
  }
}
