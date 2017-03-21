package com.gumirov.shamil.partsib;

import junit.framework.TestCase;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class SevenZipBindTest extends TestCase {
  public void test7Zip() throws Exception {
    SevenZip.initSevenZipFromPlatformJAR();
    RandomAccessFile randomAccessFile = null;
    IInArchive inArchive = null;
    try {
      randomAccessFile = new RandomAccessFile("src/data/test.full/rarfile.rar", "r");
      inArchive = SevenZip.openInArchive(null, // Choose format automatically
          new RandomAccessFileInStream(randomAccessFile));
      int l = inArchive.getNumberOfItems();
      List<String> names = Arrays.asList("rarfile2.txt", "rartxt.txt");
      assertTrue(l == names.size());
      for (int i = 0; i < l; ++i)
        assertTrue(names.contains(inArchive.getStringProperty(i, PropID.getPropIDByIndex(3))));
    } finally {
      if (inArchive != null) {
        try {
          inArchive.close();
        } catch (SevenZipException e) {
          System.err.println("Error closing archive: " + e);
        }
      }
      if (randomAccessFile != null) {
        try {
          randomAccessFile.close();
        } catch (IOException e) {
          System.err.println("Error closing file: " + e);
        }
      }
    }
  }

  public void testUnRar() throws Exception {

  }

}
