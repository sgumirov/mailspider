package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import java.io.InputStream;

/**
 * Extracts attachment object to body as inputstream. There must be exactly one attachment
 * (use this processor after SplitAttachmentsExpression). The headers are left as they were (from email message).
 */
public class EmailAttachmentProcessor implements Processor {
  protected Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

  @Override
  public void process(Exchange exchange) throws Exception {
    Message msg = exchange.getIn();
    //must be exactly one attachment (use after SplitAttachmentsExpression)
    int count = 0;
    for (String fname : msg.getAttachmentObjects().keySet()){
      if (count > 0)
        throw new RuntimeException("EmailAttachmentProcessor must be used only after SplitAttachmentsExpression, so that only one attachment per message.");
      DataHandler data = msg.getAttachmentObjects().get(fname).getDataHandler();
      byte[] s = exchange.getContext().getTypeConverter().convertTo(byte[].class, data.getInputStream());
      msg.setBody(s);
      msg.setHeader(Exchange.FILE_NAME, fname);
      log.info("Extracted attachment with name: "+fname);
      ++count;
    }
  }
}
