package com.gumirov.shamil.partsib.configuration.endpoints;

import org.apache.camel.Predicate;

import java.util.List;

public class PricehookIdTaggingRule
{
  public String id;
  public String header;
  public String contains;
  public String pricehookid;
  public List<AttachmentTaggingRule> filerules;

  public Predicate predicate;

  public String getId() {
    return id;
  }
}
