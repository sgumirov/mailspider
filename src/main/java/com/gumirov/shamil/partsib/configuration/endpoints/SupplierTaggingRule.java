package com.gumirov.shamil.partsib.configuration.endpoints;

import org.apache.camel.Predicate;

/**
 * Implements following structure:
 * <pre>
 * "id":"rule_01",
 * "header":"From",
 * "contains":"rossko",
 * "supplier-id":"10"
 * </pre>
 */
public class SupplierTaggingRule
{
  public String id;
  public String header;
  public String contains;
  public String supplierid;
  public Predicate predicate;
}
