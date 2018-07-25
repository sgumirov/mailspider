package com.gumirov.shamil.partsib.util;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import net.sf.sevenzipjbinding.util.ByteArrayStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns Li
 * @author Shamil@Gumirov.com
 * Copyright (c) 2018 by Shamil Gumirov.
 */
public class SevenZipStreamUnpacker implements Unpacker {
  private static final String PREFIX = "mailspider_";
  static Logger log = LoggerFactory.getLogger(SevenZipStreamUnpacker.class);
  private FileNameExcluder filenameExcluder;

  public SevenZipStreamUnpacker() {
  }

  public SevenZipStreamUnpacker(FileNameExcluder filenameExcluder) {
    this.filenameExcluder = filenameExcluder;
  }

  @Override
  public List<NamedResource> unpack(byte[] b) {
    IInArchive inArchive = null;
    List<NamedResource> list = new ArrayList<>();
    try {
      SevenZip.initSevenZipFromPlatformJAR();
      inArchive = SevenZip.openInArchive(null, // Choose format automatically
          new ByteArrayStream(b, false, Integer.MAX_VALUE)); //disable max size as per javadoc
      ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
      for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
        if (!item.isFolder()) {
          final String name = item.getPath();
          if (filenameExcluder != null && filenameExcluder.excludeName(name)) {
            log.info("StreamUnpacker: excluded file (reason: filenameExcluder) with path="+name);
            continue;
          }
          //Consider using Files.createTempFile() if security is envolved:
          final File tempFile = File.createTempFile(PREFIX, null);
          BufferedOutputStream bufos = new BufferedOutputStream(new FileOutputStream(tempFile));
          FileInputStream fis = new FileInputStream(tempFile);
          ExtractOperationResult result;
          //this is expected to be finished inside this blocking call
          result = item.extractSlow(new ISequentialOutStream() {
            public int write(byte[] data) {
              try {
                bufos.write(data);
              } catch (IOException e) {
                log.error("Cannot properly store extracted file, maybe too large: " + name, e);
              }
              return data.length; // consumed all
            }
          });
          bufos.flush();
          bufos.close();

          if (result == ExtractOperationResult.OK) {
            log.info(String.format("extracted file: %s len=%d", name, tempFile.length()));
            list.add(new NamedStream(name, fis, (int) tempFile.length()));
          } else {
            System.err.println("Error extracting item: " + result);
          }
        }
      }
    }catch(SevenZipException e){
      //todo proxy printstacktrace to printstacktraceExtended so that logger prints extended 7zip stacktrace
      log.error("Cannot unpack archive, see 7zipbinding: "+e.getSevenZipExceptionMessage(), e);
    }catch(Exception e){
      log.error("Cannot unpack archive, see trace", e);
      return null;
    } finally {
      if (inArchive != null) {
        try {
          inArchive.close();
        } catch (SevenZipException e) {
          log.error("Error closing archive: " + e);
        }
      }
    }
    return list;
  }

  public void setFilenameExcluder(FileNameExcluder filenameExcluder) {
    this.filenameExcluder = filenameExcluder;
  }
}
