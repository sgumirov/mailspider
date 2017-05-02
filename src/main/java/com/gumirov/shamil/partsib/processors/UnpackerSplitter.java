package com.gumirov.shamil.partsib.processors;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import net.sf.sevenzipjbinding.util.ByteArrayStream;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    byte[] bytes = exchange.getIn().getBody(byte[].class);
    List<Message> outMsgs = new ArrayList<>();
    try {
      List<NamedByteArray> files = unpack(bytes);
      for (NamedByteArray file : files) {
        Message copy = exchange.getIn().copy();
        copy.setBody(file.bytes, byte[].class);
        copy.setHeader(Exchange.FILE_NAME, file.name);
        outMsgs.add(copy);
      }
    }catch (Exception e){
      log.error("Error while unpacking archive: "+filename+String.format(", will return %d extracted files", outMsgs.size()));
    }
    return outMsgs;
  }
  
  private List<NamedByteArray> unpack(byte[] b) throws Exception {
    IInArchive inArchive = null;
    List<NamedByteArray> list = new ArrayList<>();
    try {
      SevenZip.initSevenZipFromPlatformJAR();
      inArchive = SevenZip.openInArchive(null, // Choose format automatically
          new ByteArrayStream(b, false, 102400));
      ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
      for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
        if (!item.isFolder()) {
          final String name = item.getPath();
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ExtractOperationResult result;
          result = item.extractSlow(new ISequentialOutStream() {
            public int write(byte[] data) throws SevenZipException {
              try {
                bos.write(data);
              } catch (IOException e) {
                log.error("Cannot properly store extracted file, maybe too large: " + name, e);
              }
              return data.length; // Return amount of consumed data
            }
          });

          if (result == ExtractOperationResult.OK) {
            log.info(String.format("extracted file: %s", name));
            list.add(new NamedByteArray(bos.toByteArray(), name));
          } else {
            System.err.println("Error extracting item: " + result);
          }
        }
      }
    }catch(Exception e){
      log.error("Cannot unpack archive, see trace", e);
    } finally {
      if (inArchive != null) {
        try {
          inArchive.close();
        } catch (SevenZipException e) {
          System.err.println("Error closing archive: " + e);
        }
      }
    }
    return list;
  }

  class NamedByteArray{
    byte[] bytes;
    String name;

    public NamedByteArray(byte[] bytes, String name) {
      this.bytes = bytes;
      this.name = name;
    }
  }
}


