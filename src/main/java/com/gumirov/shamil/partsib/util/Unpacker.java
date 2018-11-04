package com.gumirov.shamil.partsib.util;

import java.util.List;

/**
 * General contract for extracting files from archive and returning a {@link List} of {@link NamedResource}s
 * @author shamil@gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 * TODO (shamil): add unit-tests for unpacker
 */
public interface Unpacker {
  List<NamedResource> unpack(byte[] archive);
}
