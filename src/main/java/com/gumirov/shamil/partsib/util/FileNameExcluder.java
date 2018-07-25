package com.gumirov.shamil.partsib.util;

/**
 * Interface used when need to filter out files by name.
 * <p>Note: pay attention to REVERSED logics (TRUE means DO NOT USE FILE LATER, FALSE means USE FILE IN LATER PROCESSING).
 */
public interface FileNameExcluder {
  /**
   * Pay attention to reversed logics (TRUE means DO NOT USE FILE LATER, FALSE means USE FILE IN LATER PROCESSING).
   * @param filename file name
   * @return true to exclude file, false to use it.
   */
  boolean excludeName(String filename);
}
