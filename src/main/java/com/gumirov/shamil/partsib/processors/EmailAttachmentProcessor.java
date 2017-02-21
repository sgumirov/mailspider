package com.gumirov.shamil.partsib.processors;

import org.apache.camel.Attachment;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * Puts each attachment into file
 */
public class EmailAttachmentProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    Message msg = exchange.getIn();
    for (String name : msg.getAttachmentNames()) {

    }
  }
}
