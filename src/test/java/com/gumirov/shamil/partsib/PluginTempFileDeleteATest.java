package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;
import org.slf4j.Logger;

import javax.activation.DataHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class PluginTempFileDeleteATest
    extends AbstractMailAutomationTest
{
  List<File> tempFiles = new ArrayList<>();
  Plugin testPlugin = new PluginCreatingTempFile();

  class PluginCreatingTempFile implements Plugin {
    @Override
    public Result processFile(FileMetaData fileMetaData, Logger logger) {
      try {
        File f = File.createTempFile("test", "file");
        tempFiles.add(f);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write("hi".getBytes());
        fos.flush();
        fos.close();
        logger.info("Created TEMP FILE = "+f.getPath());
        fileMetaData.addFileToDelete(f);
        return Result.create(f);
      } catch (IOException e) {
        log.error("File creation error", e);
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void test() throws Exception {
    Map<String, DataHandler> attachments = Collections.singletonMap("a.csv",
        new DataHandler("hi there", "text/plain"));

    //send 2 messages, expect 1 notification
    launch("acceptedmail", "taglogger",
        Arrays.asList("TAG", "TAG"),
        null, 2,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        new EmailMessage("Price", "office@dinamikasveta.ru", attachments),
        new EmailMessage("Price", "office@dinamikasveta.ru", attachments)
    );
  }

  @Override
  public void assertPostConditions() throws Exception {
    super.assertPostConditions();
    assertTrue(tempFiles.size() == 2);
    for (File f : tempFiles) {
      assert (!f.exists());
    }
    log.info("PASS");
  }

  @Override
  protected List<Plugin> getPluginsList() {
    return Collections.singletonList(testPlugin);
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    PricehookIdTaggingRule rule = new PricehookIdTaggingRule();
    rule.contains = "Price";
    rule.header = "Subject";
    rule.pricehookid = "TAG";
    return Collections.singletonList(rule);
  }
}
