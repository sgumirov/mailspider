package com.gumirov.shamil.partsib.mail;

import org.apache.camel.Attachment;
import org.apache.camel.component.mail.ContentTypeResolver;
import org.apache.camel.component.mail.MailBinding;
import org.apache.camel.impl.DefaultAttachment;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * Fix for parsing nested message attachments.
 * <p>Copyright (c) 2017 by Shamil Gumirov.
 */
public class MailBindingFixNestedAttachments extends MailBinding {

  private static final Logger LOG = LoggerFactory.getLogger(MailBindingFixNestedAttachments.class);

  public MailBindingFixNestedAttachments() {
  }

  public MailBindingFixNestedAttachments(HeaderFilterStrategy headerFilterStrategy, ContentTypeResolver contentTypeResolver) {
    super(headerFilterStrategy, contentTypeResolver);
  }

  @Override
  protected void extractAttachmentsFromMultipart(Multipart mp, Map<String, Attachment> map) throws MessagingException, IOException {
    for (int i = 0; i < mp.getCount(); i++) {
      Part part = mp.getBodyPart(i);
      LOG.trace("Part #" + i + ": " + part);

      if (part.isMimeType("multipart/*")) {
        LOG.trace("Part #" + i + ": is mimetype: multipart/*");
        extractAttachmentsFromMultipart((Multipart) part.getContent(), map);
      } else if (part.isMimeType("MESSAGE/*")){
        extractAttachmentsFromMail((Message) part.getContent(), map);
      } else {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();

        if (LOG.isTraceEnabled()) {
          LOG.trace("Part #{}: Disposition: {}", i, disposition);
          LOG.trace("Part #{}: Description: {}", i, part.getDescription());
          LOG.trace("Part #{}: ContentType: {}", i, part.getContentType());
          LOG.trace("Part #{}: FileName: {}", i, fileName);
          LOG.trace("Part #{}: Size: {}", i, part.getSize());
          LOG.trace("Part #{}: LineCount: {}", i, part.getLineCount());
        }

        if (validDisposition(disposition, fileName)
            || fileName != null) {
          LOG.debug("Mail contains file attachment: {}", fileName);
          if (!map.containsKey(fileName)) {
            // Parts marked with a disposition of Part.ATTACHMENT are clearly attachments
            DefaultAttachment camelAttachment = new DefaultAttachment(part.getDataHandler());
            @SuppressWarnings("unchecked")
            Enumeration<Header> headers = part.getAllHeaders();
            while (headers.hasMoreElements()) {
              Header header = headers.nextElement();
              camelAttachment.addHeader(header.getName(), header.getValue());
            }
            map.put(fileName, camelAttachment);
          } else {
            LOG.warn("Cannot extract duplicate file attachment: {}.", fileName);
          }
        }
      }
    }
  }

  protected boolean validDisposition(String disposition, String fileName) {
    return disposition != null
        && fileName != null
        && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE));
  }
}
