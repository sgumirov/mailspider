package com.gumirov.shamil.partsib;

import java.io.InputStream;
import java.util.Map;

public interface AttachmentVerifier {
  boolean verify(Map<String, InputStream> attachments);
}
