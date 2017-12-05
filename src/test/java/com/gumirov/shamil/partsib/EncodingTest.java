package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.util.Util;
import com.sun.xml.internal.ws.encoding.DataSourceStreamingDataHandler;
import com.sun.xml.internal.ws.util.ByteArrayDataSource;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class EncodingTest extends AbstractMailAutomationTest {
  @Test
  public void test() throws Exception {
    Map<String, DataHandler> attachment = new HashMap<>();
    final String name = "944-xtr.xls", ct = "application/vnd.ms-excel", tag = "944.0.main";
    byte[] d = Util.readFully(getClass().getClassLoader().getResourceAsStream(name));
    DataSource ds = new ByteArrayDataSource(d, ct);
    attachment.put(name, new DataSourceStreamingDataHandler(ds));

    setAttachmentVerifier(new AttachmentVerifier(){
      @Override
      public boolean verify(Map<String, InputStream> attachments){
        for (String f : attachment.keySet()) {
          try {
            System.out.println(new String(Util.readFully(attachments.get(f))));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        return true;
      }
    });

    launch("acceptedmail", "taglogger",
        Arrays.asList(tag),
        null, 1, "direct:emailreceived",
        new EmailMessage("subj", "natalia.sh@ivers.ru", attachment)
    );
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("prod_rules.json");
  }
}
