package com.gumirov.shamil.partsib.mail;

import org.apache.camel.Attachment;
import org.apache.camel.component.mail.ContentTypeResolver;
import org.apache.camel.impl.DefaultAttachment;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;

/**
 * Fix for parsing nested message attachments.
 *
 * <p>Copyright (c) 2017 by Shamil Gumirov.
 */
public class MailBindingFixNestedAttachments extends MailBindingFixSubjectDecode {

  private static final Logger LOG = LoggerFactory.getLogger(MailBindingFixNestedAttachments.class);

  public MailBindingFixNestedAttachments() {
  }

  public MailBindingFixNestedAttachments(HeaderFilterStrategy headerFilterStrategy, ContentTypeResolver contentTypeResolver) {
    super(headerFilterStrategy, contentTypeResolver);
  }

  //fix issue with bare attachment

  @Override
  public void extractAttachmentsFromMail(Message message, Map<String, Attachment> attachments)
      throws MessagingException, IOException {
    Object content = message.getContent();
    if (content instanceof Multipart) {
      extractAttachmentsFromMultipart((Multipart) content, attachments);
    } else if (content != null && message.getDisposition() != null) {
      String disposition = message.getDisposition().toLowerCase();

      if (disposition.contains(Part.ATTACHMENT) || disposition.contains(Part.INLINE)) {
        LOG.trace("No attachments was extracted using default MailBinding class, extract attachment without body.");
        String filename = mimeDecodeText(message.getFileName());

        if (LOG.isTraceEnabled()) {
          LOG.trace("ContentType: {}", message.getContentType());
          LOG.trace("Disposition: {}", disposition);
          LOG.trace("Description: {}", message.getDescription());
          LOG.trace("FileName: {}", (filename == null ? "null" : filename));
          LOG.trace("Size: {}", message.getSize());
          LOG.trace("LineCount: {}", message.getLineCount());
        }

        if (filename != null && !attachments.containsKey(filename)) {
          LOG.debug("Extracting file attachment: {}", filename);
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          message.getDataHandler().writeTo(bos);
          DefaultAttachment camelAttachment = new DefaultAttachment(new DataHandler(bos.toByteArray(),
              message.getContentType()));
          Enumeration<Header> headers = message.getAllHeaders();
          while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            camelAttachment.setHeader(header.getName(), header.getValue());
          }
          attachments.put(filename, camelAttachment);
        } else {
          LOG.warn("Already extracted same filename attachment: {}.", filename);
        }
      }
    }
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
        LOG.trace("Part #" + i + ": is mimetype: MESSAGE/*");
        try {
          extractAttachmentsFromMail((MimeMessage) part.getContent(), map);
        } catch (Exception e) {
          LOG.error("Error extracting attachments from part #"+i+": attachment name="+part.getFileName(), e);
        }
      } else {
        String disposition = part.getDisposition();
        String fileName = mimeDecodeText(part.getFileName());

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
          if (!map.containsKey(fileName)) {
            // Parts marked with a disposition of Part.ATTACHMENT are clearly attachments
            DefaultAttachment camelAttachment = new DefaultAttachment(part.getDataHandler());
            Enumeration<Header> headers = part.getAllHeaders();
            while (headers.hasMoreElements()) {
              Header header = headers.nextElement();
              camelAttachment.addHeader(header.getName(), header.getValue());
            }
            map.put(fileName, camelAttachment);
            LOG.trace("Extracted file attachment: {}", fileName);
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

  private String mimeDecodeText(String text) throws UnsupportedEncodingException {
    if (text == null) return null;
    return MimeUtility.decodeText(text);
  }
}
