package com.gumirov.shamil.partsib.processors;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Just a bean with method returning List(Message)
 */
public class UnpackerSplitter {
  static Logger log = LoggerFactory.getLogger(UnpackerSplitter.class);

  public List<Message> unpack(Exchange exchange) {
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
    InputStream fis = exchange.getIn().getBody(InputStream.class);
    try {
      unpack(fis);
    }catch (Exception e){
      log.error("Cannot unpack archive: "+filename);
    }
  }
  
  private List<NamedByteArray> unpack(InputStream is) throws Exception {
    RandomAccessFile randomAccessFile = null;
    IInArchive inArchive = null;
    List<NamedByteArray> list = new ArrayList<>();
    try {
      SevenZip.initSevenZipFromPlatformJAR();
      randomAccessFile = new RandomAccessFile("src/data/test.full/rarfile.rar", "r");
      inArchive = SevenZip.openInArchive(null, // Choose format automatically
          new RandomAccessFileInStream(randomAccessFile));
      int l = inArchive.getNumberOfItems();
      for (int i = 0; i < l; ++i) {
        String name = inArchive.getStringProperty(i, PropID.getPropIDByIndex(3));
      }
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
    return list;
  }
}


class NamedByteArray{
  byte[] bytes;
  String name;

  public NamedByteArray(byte[] bytes, String name) {
    this.bytes = bytes;
    this.name = name;
  }
}
