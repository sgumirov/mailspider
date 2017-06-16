package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
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
      try {
        if (fname == null) {
          log.warn("Message has empty filename for attachment: from="+exchange.getIn().getHeader("From", String.class));
          exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
        }
        Attachment a = msg.getAttachmentObjects().get(fname);
        DataHandler data = a.getDataHandler();
        byte[] s = exchange.getContext().getTypeConverter().convertTo(byte[].class, data.getContent());
        msg.setBody(s);
        fname = MimeUtility.decodeText(fname);
        msg.setHeader(Exchange.FILE_NAME, fname);
        log.info("Extracted attachment name: "+fname);
      } catch (Exception e) {
        log.error("Cannot process attachment: "+fname, e);
      }
      ++count;
    }
  }
}
