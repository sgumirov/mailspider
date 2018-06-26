package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.*;
import com.partsib.mailspider.plugins.ExcelToCsvConverterPlugin;
import org.junit.Test;

import javax.activation.DataHandler;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class AvtotrialTest extends AbstractMailAutomationTest {
  @Override
  @Test
  public void test() throws Exception {
    //this email has large attachment (~20mb)
    RawEmailMessage email = new RawEmailMessage(getClass().getClassLoader().getResourceAsStream("avtotrial/original_msg-4.txt"));
    launch("acceptedmail", "taglogger",
        Collections.singletonList("998.0.main"),
        null, 1,
        EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0)), //send through first endpoint
        email
    );
  }

  @Override
  protected List<Plugin> getPluginsList() {
    return Collections.singletonList(new ExcelToCsvConverterPlugin());
  }

  @Override
  public List<PricehookIdTaggingRule> getTagRules() {
    return loadTagRules("prod_rules.json");
  }
}
