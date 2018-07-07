package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.internet.MimeUtility;

import java.io.InputStream;

import static com.gumirov.shamil.partsib.MainRouteBuilder.MID;

/**
 * Extracts attachment object to body as inputstream. There must be exactly one attachment
 * (use this processor after SplitAttachmentsExpression). The headers are left as they were (from email message).
 */
public class EmailAttachmentProcessor implements Processor {
  protected Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

  @Override
  public void process(Exchange exchange) {
    Message msg = exchange.getIn();
    //must be exactly one attachment (use after SplitAttachmentsExpression)
    int count = 0;
    if (msg.getAttachmentObjects().size() == 0) {
      log.info("["+exchange.getIn().getHeader(MID)+"]"+" Email has no attachments");
    } else for (String fname : msg.getAttachmentObjects().keySet()){
      if (count > 0) {
        log.error("["+exchange.getIn().getHeader(MID)+"]"+" EmailAttachmentProcessor must be used after SplitAttachmentsExpression: only 1 attachment can be decoded.");
        throw new RuntimeException("[" + exchange.getIn().getHeader(MID) + "]" + " EmailAttachmentProcessor must be used only after SplitAttachmentsExpression, so that only one attachment per message.");
      }
      try {
        if (fname == null) {
          log.warn("["+exchange.getIn().getHeader(MID)+"]"+" Message has empty filename for attachment: from="+exchange.getIn().getHeader("From", String.class));
          exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
        }
        Attachment a = msg.getAttachmentObjects().get(fname);
        DataHandler data = a.getDataHandler();
//        byte[] s = exchange.getContext().getTypeConverter().convertTo(byte[].class, data.getContent());
//        msg.setBody(s);
        InputStream is = (InputStream) data.getContent();
        exchange.getIn().setHeader(MainRouteBuilder.LENGTH_HEADER, is.available());
        msg.setBody(is);
        fname = MimeUtility.decodeText(fname); //let it fall! nullpointer will be caught below
        msg.setHeader(Exchange.FILE_NAME, fname);
        log.info("["+exchange.getIn().getHeader(MID)+"]"+" Extracted attachment name: "+fname);
      } catch (Exception e) {
        log.error("["+exchange.getIn().getHeader(MID)+"]"+" Cannot process attachment: "+fname, e);
      }
      ++count;
    }
  }
}
