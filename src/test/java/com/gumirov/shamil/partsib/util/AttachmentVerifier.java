package com.gumirov.shamil.partsib.util;

import java.io.InputStream;
import java.util.Map;

public abstract class AttachmentVerifier {
  public boolean verifyContents(Map<String, InputStream> attachments){return true;}
  public boolean verifyTags(Map<String, String> attachments){return true;}
}
