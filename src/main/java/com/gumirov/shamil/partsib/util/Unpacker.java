package com.gumirov.shamil.partsib.util;

import java.util.List;

/**
 * Extracts files from archive, creating {@link List} of {@link NamedResource}
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public interface Unpacker {
  List<NamedResource> unpack(byte[] archive);
}
