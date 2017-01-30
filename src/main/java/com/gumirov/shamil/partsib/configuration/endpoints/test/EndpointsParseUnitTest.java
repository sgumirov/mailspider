package com.gumirov.shamil.partsib.configuration.endpoints.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumirov.shamil.partsib.configuration.endpoints.EmailRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoint;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class EndpointsParseUnitTest {
  
  @Test
  public void testEmailRules() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getResourceAsStream("test_email_rules.json"), Charset.defaultCharset());
    List<EmailRule> rules = mapper.readValue(json, new TypeReference<List<EmailRule>>() {});
    Assert.assertTrue(rules.size() == 2);
    Assert.assertTrue("/supplier2@mail\\.ru/".equals(rules.get(1).regexp));
    Assert.assertTrue("rule_02".equals(rules.get(1).id));
    Assert.assertTrue("email_supplier_id_2".equals(rules.get(1).apply_id));
  }

  @Test
  public void testEndpoints() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = IOUtils.toString(getClass().getResourceAsStream("test_endpoints.json"), Charset.defaultCharset());
    Endpoints endpoints = mapper.readValue(json, Endpoints.class);
    Assert.assertTrue(endpoints.ftp.size() == 2);
    Assert.assertTrue(endpoints.http.size() == 2);
    Assert.assertTrue(endpoints.email.size() == 2);
    Assert.assertEquals("ftp://192.168.0.1/files/1.zip", endpoints.ftp.get(0).url);
    Assert.assertEquals("ftp_supplier_01", endpoints.ftp.get(0).id);
    Assert.assertEquals("username", endpoints.ftp.get(0).user);
    Assert.assertEquals("password", endpoints.ftp.get(0).pwd);
  }
}
