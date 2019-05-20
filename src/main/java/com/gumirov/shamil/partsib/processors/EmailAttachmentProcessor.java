package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.internet.MimeUtility;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.MESSAGE_ID_HEADER;

/**
 * Extracts attachment object to body as inputstream. There must be exactly one attachment
 * (use this processor after SplitAttachmentsExpression). The headers are left as they were (from email message).
 */
public class EmailAttachmentProcessor implements Processor {
  protected Logger log = LoggerFactory.getLogger(getClass().getSimpleName());
  private boolean configDeleteTempFilesOnExit;

  public EmailAttachmentProcessor(boolean configDeleteTempFilesOnExit) {
    this.configDeleteTempFilesOnExit = configDeleteTempFilesOnExit;
  }

  @Override
  public void process(Exchange exchange) {
    Message msg = exchange.getIn();
    //must be exactly one attachment (use after SplitAttachmentsExpression)
    int count = 0;
    if (msg.getAttachmentObjects().size() == 0) {
      log.info("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Email has no attachments");
    } else for (String fname : msg.getAttachmentObjects().keySet()){
      if (count > 0) {
        log.error("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" EmailAttachmentProcessor must be used after SplitAttachmentsExpression: only 1 attachment can be decoded.");
        throw new RuntimeException("[" + exchange.getIn().getHeader(MESSAGE_ID_HEADER) + "]" + " EmailAttachmentProcessor must be used only after SplitAttachmentsExpression, so that only one attachment per message.");
      }
      try {
        if (fname == null) {
          log.warn("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Message has empty filename for attachment: from="+exchange.getIn().getHeader("From", String.class));
          exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
        }
        Attachment a = msg.getAttachmentObjects().get(fname);
        DataHandler data = a.getDataHandler();
        InputStream is;
        Object content = data.getContent();
        long len;
        if (content instanceof InputStream) {
          is = (InputStream) content;
          File tempFile = Util.dumpTemp(is);
          if (configDeleteTempFilesOnExit) tempFile.deleteOnExit();
          msg.setBody(new FileInputStream(tempFile));
          len = tempFile.length();
        } else if (content instanceof String) {
          byte[] b = ((String)content).getBytes("UTF-8");
          len = b.length;
          is = new ByteArrayInputStream(b);
          msg.setBody(is);
        } else if (content instanceof byte[]) {
          byte[] b = (byte[]) data.getContent();
          File tempFile = Util.dumpTemp(b);
          if (configDeleteTempFilesOnExit) tempFile.deleteOnExit();
          msg.setBody(new FileInputStream(tempFile));
          len = tempFile.length();
        } else {
          throw new IllegalArgumentException("Cannot process attachment with type: "+content.getClass().getSimpleName());
        }
        exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.LENGTH_HEADER, len);
        fname = MimeUtility.decodeText(fname); //let it fall! nullpointer will be caught below
        msg.setHeader(Exchange.FILE_NAME, fname);
        log.info("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Extracted attachment name: "+fname);
      } catch (Exception e) {
        log.error("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Cannot process attachment: "+fname, e);
      }
      ++count;
    }
  }
}
