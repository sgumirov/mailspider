package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.configuration.endpoints.AttachmentTaggingRule;
import com.gumirov.shamil.partsib.configuration.endpoints.PricehookIdTaggingRule;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.MESSAGE_ID_HEADER;

/**
 * <p>Note: this processor could override pricehook ID (tag) for this attachment.
 * <p>Tags attachment using pricehook email rule's 'filerules'.
 * So this processor needs to have a rule object used for tagging this whole email to be set in
 * header {@link MainRouteBuilder.HeaderKeys#PRICEHOOK_RULE}.
 */
public class AttachmentTaggerProcessor implements Processor {
  public static final String ID = "AttachmentPricehookTagger";
  private final Logger log = LoggerFactory.getLogger(AttachmentTaggerProcessor.class.getSimpleName());

  @Override
  public void process(Exchange exchange) {
    if (null != exchange.getIn().getHeader(MainRouteBuilder.HeaderKeys.PRICEHOOK_RULE)){
      PricehookIdTaggingRule rule = exchange.getIn().getHeader(MainRouteBuilder.HeaderKeys.PRICEHOOK_RULE, PricehookIdTaggingRule.class);
      if (rule.filerules != null) {
        String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
        for (AttachmentTaggingRule r : rule.filerules) {
          if (filename.toUpperCase().contains(r.getNamecontains().toUpperCase())){
            exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.PRICEHOOK_ID_HEADER, r.pricehookid);
            log.info("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Pricehook ID (Tag) was set for attachment by filename (file="+filename+") to "+r.pricehookid);
          }
        }
      }
    }
  }
}
