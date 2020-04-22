package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.EmailAcceptRule;
import com.gumirov.shamil.partsib.configuration.endpoints.Endpoints;
import com.gumirov.shamil.partsib.util.JsonParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class EndpointsParseUnitTest {
  
  final String json = "[\n" +
      "  {\n" +
      "    \"id\":\"rule_01\",\n" +
      "    \"header\":\"From\",\n" +
      "    \"contains\":\"@gmail.com\"\n" +
      "  }\n" +
      "]\n";
  
  @Test
  public void testEmailRules() throws IOException {
    List<EmailAcceptRule> rules = new JsonParser<EmailAcceptRule>().parseList(json, EmailAcceptRule.class);
    Assert.assertTrue(rules.size() == 1);
    Assert.assertTrue("From".equals(rules.get(0).header));
    Assert.assertTrue("rule_01".equals(rules.get(0).id));
    Assert.assertTrue("@gmail.com".equals(rules.get(0).contains));
  }

  String endpointsS = "{\n" +
      "  \"ftp\":[\n" +
      "    {\n" +
      "      \"id\":\"ftp_supplier_01\",\n" +
      "      \"url\":\"ftp://192.168.0.1/files/1.zip\",\n" +
      "      \"user\":\"username\",\n" +
      "      \"pwd\":\"password\",\n" +
      "      \"delay\":\"60000\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\":\"ftp_supplier_dir\",\n" +
      "      \"url\":\"ftp://127.0.0.1/test/\",\n" +
      "      \"user\":\"anonymous\",\n" +
      "      \"pwd\":\"a@google.com\",\n" +
      "      \"delay\":\"60000\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"http\":[\n" +
      "    {\n" +
      "      \"id\": \"HTTP_Optima\",\n" +
      "      \"factory\": \"com.gumirov.shamil.partsib.factories.OptimaRouteFactory\",\n" +
      "      \"url\": \"https://optma.ru/index.php?r=site/products\",\n" +
      "      \"user\": \"partsib\",\n" +
      "      \"pwd\": \"partsib5405\",\n" +
      "      \"delay\": \"60000\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"email\":[\n" +
      "  ]\n" +
      "}\n";

  @Test
  public void testEndpoints() throws IOException {
    String json = endpointsS; //IOUtils.toString(getClass().getResourceAsStream("test_local_endpoints.json"), Charset.defaultCharset());
    Endpoints endpoints = new JsonParser<Endpoints>().parse(json, Endpoints.class);
    Assert.assertTrue(endpoints.ftp.size() == 2);
    Assert.assertTrue(endpoints.http.size() == 1);
    Assert.assertTrue(endpoints.email.size() == 0);
    Assert.assertEquals("ftp://192.168.0.1/files/1.zip", endpoints.ftp.get(0).url);
    Assert.assertEquals("ftp_supplier_01", endpoints.ftp.get(0).id);
    Assert.assertEquals("username", endpoints.ftp.get(0).user);
    Assert.assertEquals("password", endpoints.ftp.get(0).pwd);
    Assert.assertEquals("60000", endpoints.ftp.get(0).delay);
  }

  @Test
  public void testMultipleEndpointsConfig() throws IOException {
    Endpoints endpoints = new JsonParser<Endpoints>().load("multiple_endpoints/multiple_endpoints.json", Endpoints.class);
    Assert.assertEquals(2, endpoints.ftp.size());
    Assert.assertEquals(1, endpoints.http.size());
    Assert.assertEquals(2, endpoints.email.size());
    Assert.assertEquals("ftp://192.168.0.1/files/1.zip", endpoints.ftp.get(0).url);
    Assert.assertEquals("ftp_supplier_01", endpoints.ftp.get(0).id);
    Assert.assertEquals("username", endpoints.ftp.get(0).user);
    Assert.assertEquals("password", endpoints.ftp.get(0).pwd);
    Assert.assertEquals("60000", endpoints.ftp.get(0).delay);

    Assert.assertEquals("ftp://127.0.0.1/test/", endpoints.ftp.get(1).url);
    Assert.assertEquals("ftp_supplier_dir", endpoints.ftp.get(1).id);
    Assert.assertEquals("anonymous", endpoints.ftp.get(1).user);
    Assert.assertEquals("a@google.com", endpoints.ftp.get(1).pwd);
    Assert.assertEquals("60000", endpoints.ftp.get(1).delay);

    Assert.assertEquals("email_inbox_gmail_03", endpoints.email.get(0).id);
    Assert.assertEquals("email_inbox_gmail_08", endpoints.email.get(1).id);
  }
}
