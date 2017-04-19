package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public interface PricehookIdTaggingRulesConfigProvider {
  List<PricehookIdTaggingRule> loadPricehookConfig(String url) throws IOException;
}
