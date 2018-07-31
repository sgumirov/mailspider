package com.gumirov.shamil.partsib.mail;

import org.apache.camel.Exchange;
import org.apache.camel.component.mail.ContentTypeResolver;
import org.apache.camel.component.mail.MailBinding;
import org.apache.camel.component.mail.MailEndpoint;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * Fix for incorrect Subject encoding, related public bugs:
 * https://github.com/neomutt/neomutt/issues/1015
 * https://bugzilla.mozilla.org/show_bug.cgi?id=493544
 */
public class MailBindingFixSubjectDecode extends MailBinding {
  private static final Logger LOG = LoggerFactory.getLogger(MailBindingFixSubjectDecode.class);

  public MailBindingFixSubjectDecode() {
  }

  public MailBindingFixSubjectDecode(HeaderFilterStrategy headerFilterStrategy, ContentTypeResolver contentTypeResolver) {
    super(headerFilterStrategy, contentTypeResolver);
  }

  /**
   * Fix subject decoding issue (RFC-822 line break right at 2-bytes unicode char).
   * @param mail original message
   */
  @Override
  protected Map<String, Object> extractHeadersFromMail(Message mail, Exchange exchange) throws MessagingException, IOException {
    Map<String, Object> headers = super.extractHeadersFromMail(mail, exchange);
    if (((MailEndpoint) exchange.getFromEndpoint()).getConfiguration().isMimeDecodeHeaders()){
      StringBuilder sb = new StringBuilder();
      for (Enumeration h = mail.getAllHeaders(); h.hasMoreElements(); ) {
        String k = ((Header) h.nextElement()).getName();
        if ("subject".equals(k.toLowerCase())) {
          for (String v : mail.getHeader(k)){
            if (sb.length()>0) sb.append("\n");
            sb.append(v);
          }
        }
      }
      String subjectEncoded = sb.toString();
      headers.put("Subject", HeaderDecoder.decode(subjectEncoded));
    }
    return headers;
  }
}
