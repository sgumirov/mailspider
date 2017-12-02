package com.gumirov.shamil.partsib;

import javax.mail.Message;
import java.util.Map;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class MessageBindingUnitSuiteTest {

  void setDisposition(String diposition, Message m) {}
  void setMultipart(boolean isMultipart, int num, Message m) {}
  void setTextBody(String textBody, Message m) {}
  void setAttachments(Map<String, byte[]> attachments, boolean asParts, Message m) {}
}
