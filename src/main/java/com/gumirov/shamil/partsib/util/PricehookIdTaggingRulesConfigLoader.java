package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.Exchange;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface PricehookIdTaggingRulesConfigLoader {
  List<PricehookIdTaggingRule> loadPricehookConfig(String url, Exchange exchange) throws IOException;
}
