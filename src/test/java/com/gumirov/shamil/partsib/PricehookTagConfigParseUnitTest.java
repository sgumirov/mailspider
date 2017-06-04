package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * config:<pre>
 *
 [
 {
 "id":"rule_01",
 "header":"From",
 "contains":"rossko",
 "pricehookid":"10",
 "by-attachment": [
 {
 "namecontains": "_NSK_1",
 "pricehookid": "10.1"
 },
 {
 "namecontains": "_NSK_2",
 "pricehookid": "10.2"
 },
 {
 "namecontains": "_MOSCOW_6",
 "pricehookid": "10.6"
 },
 {
 "namecontains": "_MOSCOW_15",
 "pricehookid": "10.5"
 }
 ]
 },
 {
 "id":"rule_02",
 "header":"From",
 "contains":"test123",
 "pricehookid":"11"
 }
 ]
 * </pre>
 */
public class PricehookTagConfigParseUnitTest {
  String config = "[\n" +
      " {\n" +
      " \"id\":\"rule_01\",\n" +
      " \"header\":\"From\",\n" +
      " \"contains\":\"rossko\",\n" +
      " \"pricehookid\":\"10\",\n" +
      " \"filerules\": [\n" +
      " {\n" +
      " \"namecontains\": \"_NSK_1\",\n" +
      " \"pricehookid\": \"10.1\"\n" +
      " },\n" +
      " {\n" +
      " \"namecontains\": \"_NSK_2\",\n" +
      " \"pricehookid\": \"10.2\"\n" +
      " },\n" +
      " {\n" +
      " \"namecontains\": \"_MOSCOW_6\",\n" +
      " \"pricehookid\": \"10.6\"\n" +
      " },\n" +
      " {\n" +
      " \"namecontains\": \"_MOSCOW_15\",\n" +
      " \"pricehookid\": \"10.15\"\n" +
      " }\n" +
      " ]\n" +
      " },\n" +
      " {\n" +
      " \"id\":\"rule_02\",\n" +
      " \"header\":\"Subject\",\n" +
      " \"contains\":\"\\\"test123\\\"\",\n" +
      " \"pricehookid\":\"11\"\n" +
      " },\n" +
      "{\n" +
      "    \"id\": \"rule_977_498\",\n" +
      "    \"header\": \"Subject\",\n" +
      "    \"contains\": \"Прайс-лист ООО \\\"Мастер Сервис\\\" наличие Новосибирск\",\n" +
      "    \"pricehookid\": \"977.0.nsk\"\n" +
      "  }"+
      " ]\n";

  @Test
  public void testConfigParse() throws IOException {
    List<PricehookIdTaggingRule> rules = MainRouteBuilder.parseTaggingRules(config);
    Map<String, PricehookIdTaggingRule> rulesMap = rules.stream().collect(Collectors.toMap(
        PricehookIdTaggingRule::getId,
        Function.identity()
    ));
    Assert.assertTrue(rulesMap.containsKey("rule_01"));
    Assert.assertTrue(rulesMap.containsKey("rule_02"));
    Assert.assertTrue(rulesMap.containsKey("rule_977_498"));
    Assert.assertTrue(rulesMap.get("rule_01").filerules != null);
    Assert.assertTrue(rulesMap.get("rule_01").filerules.size() == 4);
    List<AttachmentTaggingRule> file01rules = rulesMap.get("rule_01").filerules;
    Map<String, AttachmentTaggingRule> filerulesMap = file01rules.stream().collect(Collectors.toMap(
        AttachmentTaggingRule::getNamecontains,
        Function.identity()
    ));
    Assert.assertTrue(filerulesMap.containsKey("_NSK_1"));
    Assert.assertTrue(filerulesMap.containsKey("_NSK_2"));
    Assert.assertTrue(filerulesMap.containsKey("_MOSCOW_6"));
    Assert.assertTrue(filerulesMap.containsKey("_MOSCOW_15"));
    Assert.assertTrue("10.1".equals(filerulesMap.get("_NSK_1").pricehookid));
    Assert.assertTrue("10.2".equals(filerulesMap.get("_NSK_2").pricehookid));
    Assert.assertTrue("10.6".equals(filerulesMap.get("_MOSCOW_6").pricehookid));
    Assert.assertTrue("10.15".equals(filerulesMap.get("_MOSCOW_15").pricehookid));

    PricehookIdTaggingRule r02 = rulesMap.get("rule_02");
    Assert.assertTrue(r02.contains.equals("\"test123\""));

    PricehookIdTaggingRule rule_977_498 = rulesMap.get("rule_977_498");
    Assert.assertTrue(rule_977_498.contains.equals("Прайс-лист ООО \"Мастер Сервис\" наличие Новосибирск"));
  }
}
