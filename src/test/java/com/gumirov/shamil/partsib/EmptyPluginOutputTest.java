package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import com.gumirov.shamil.partsib.plugins.FileMetaData;
import com.gumirov.shamil.partsib.plugins.Plugin;
import com.gumirov.shamil.partsib.util.EmailMessage;
import com.gumirov.shamil.partsib.util.EndpointSpecificUrl;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.LENGTH_HEADER;

public class EmptyPluginOutputTest extends AbstractMailAutomationTest {
  @Test
  public void testEmptyPluginOutput() throws Exception {
    EmailMessage email = new EmailMessage("test", "no@ivers.ru",
        makeAttachment("Прайс ру.csv"));
    launch(null, null, 1, Collections.singletonMap(email, getEndpoint()));
  }

  private String getEndpoint() {
    return EndpointSpecificUrl.apply("direct:emailreceived", getEmailEndpoints().get(0));
  }

  @Override
  public List<PricehookIdTaggingRule> getPricehookRules() {
    return loadTagRules("test_rules.json");
  }

  @Override
  protected List<Plugin> getPluginsList() {
    return Collections.singletonList(new EmptyOutputPlugin());
  }
}

class EmptyOutputPlugin implements Plugin {
  @Override
  public Result processFile(FileMetaData fileMetaData, Logger logger) throws Exception {
    ByteArrayInputStream b = new ByteArrayInputStream(new byte[0]);
    fileMetaData.headers.put(LENGTH_HEADER, 100);
    return Result.create(b);
  }
}