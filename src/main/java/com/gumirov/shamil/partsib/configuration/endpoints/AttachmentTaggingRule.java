package com.gumirov.shamil.partsib.configuration.endpoints;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 26/5/2017 Time: 14:30<br/>
 */
public class AttachmentTaggingRule {
  public String namecontains;
  public String pricehookid;

  public AttachmentTaggingRule() {
    //needed for jackson JSON parser
  }

  public AttachmentTaggingRule(String namecontains, String pricehookid) {
    this.namecontains = namecontains;
    this.pricehookid = pricehookid;
  }

  public String getNamecontains() {
    return namecontains;
  }

  @Override
  public String toString() {
    return "AttachmentTaggingRule{" +
        "namecontains='" + namecontains + '\'' +
        ", pricehookid='" + pricehookid + '\'' +
        '}';
  }
}
