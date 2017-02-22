package com.gumirov.shamil.partsib.test;

import com.gumirov.shamil.partsib.MailSpiderRouteBuilder;
import com.gumirov.shamil.partsib.configuration.Configurator;
import com.gumirov.shamil.partsib.configuration.ConfiguratorFactory;
import com.gumirov.shamil.partsib.util.FileNameIdempotentRepoManager;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.File;
import java.io.IOException;

/**
 * Automation FTP endpoint test with local FTP daemon
 */
public class MainRouteTest {

  static final String ftpDir = "/tmp/test";
  static final String resDir = "src/data/test";
  static Configurator config = new ConfiguratorFactory().getConfigurator();

  @Before
  public void setupFTP() throws IOException {
    FileUtils.deleteDirectory(new File(ftpDir));
    FileUtils.copyDirectory(new File(resDir), new File(ftpDir));
    new File(config.get("idempotent.repo")).delete();
    config = new ConfiguratorFactory().getConfigurator();
  }

  @Test
  public void test() throws Exception{
    CamelContext context = new DefaultCamelContext();
    context.setMessageHistory(true);
    context.addRoutes(createRouteBuilder());
    context.start();
    Thread.sleep(10000000);
  }

  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new MailSpiderRouteBuilder(config);
  }

  public static void main(String[] args) {
    Result result = JUnitCore.runClasses(MainRouteTest.class);

    for (Failure failure : result.getFailures()) {
      System.out.println(failure.toString());
    }

    System.out.println(result.wasSuccessful());
  }
}
