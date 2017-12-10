package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.Util;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import java.util.*;

/**
 * @author Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class EncodingTest extends AbstractMailAutomationTest {
  @Test
  public void test() throws Exception {
    Map<String, DataHandler> attachment = new HashMap<>();
    final String name = "944-xtr.xls", ct = "text/csv"/*"application/vnd.ms-excel"*/, tag = "944.0.main";
    final String contents = "привет на русском.";
    byte[] d = contents.getBytes("UTF-8"); //Util.readFully(getClass().getClassLoader().getResourceAsStream(name));
    DataSource ds = new ByteArrayDataSource(d, ct);
    attachment.put(name, new DataHandler(ds));

    setAttachmentVerifier(attachments -> {
      for (String f : attachments.keySet()) {
        try {
          return contents.equals(new String(Util.readFully(attachments.get(f)), "UTF-8"));
        } catch (Exception e) {
          log.error("Cannot read data", e);
        }
      }
      return false;
    });

    launch("acceptedmail", "taglogger",
        Collections.singletonList(tag),
        null, 1, "direct:emailreceived",
        new EmailMessage("subj", "natalia.sh@ivers.ru", attachment)
    );
  }

  @Override
  protected List<Plugin> getPluginsList() {
    //todo return list of plugins to test here
    return super.getPluginsList();
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagsFile("prod_rules.json");
  }
}
