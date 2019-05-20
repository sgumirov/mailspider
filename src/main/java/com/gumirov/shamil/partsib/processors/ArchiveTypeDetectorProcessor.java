package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.FileNameExcluder;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static com.gumirov.shamil.partsib.MainRouteBuilder.HeaderKeys.MESSAGE_ID_HEADER;

/**
 * Supported archive singature detector. If no {@link InputStream#mark(int)} is supported, then caches the whole stream
 * to disk and closes original stream.
 * <p>Supported singatures:
 * <pre>
 * gzip:     1f 8b
 * rar >1.5: 52 61 72 21 1A 07 00
 * rar >5.0: 52 61 72 21 1A 07 01 00
 * zip:      50 4B 03 04
 * 7z:       37 7a bc af 27 1c
 * </pre>
 * (c) 2017-2018 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 22:52<br/>
 */
public class ArchiveTypeDetectorProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(ArchiveTypeDetectorProcessor.class);
  
  private FileNameExcluder excluder;

  public ArchiveTypeDetectorProcessor(FileNameExcluder excluder) {
    this.excluder = excluder;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    final int SIGNATURE_LENGTH = 8;
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);

    boolean b;
    if (filename != null && excluder != null && (b = excluder.excludeName(filename.toLowerCase()))) {
      logger.info("[" + exchange.getIn().getHeader(MESSAGE_ID_HEADER) + "]" + " Archive Detection disabled for this file (" + filename + ")" + (b ? ". FileNameExcluder was used" : ""));
      return;
    }

    byte[] signature = new byte[SIGNATURE_LENGTH];
    Object body = exchange.getIn().getBody();

    if (body instanceof byte[]) {
      System.arraycopy(body, 0, signature, 0, Math.min(signature.length, ((byte[]) body).length));
    } else if (body instanceof InputStream){
      InputStream is = (InputStream) body;
      if (!is.markSupported()) {
        logger.info("WARNING: mark is not supported for attachment stream. Caching stream to disk");
        //cache stream on disk
        File dump = Util.dumpTemp(is);
        is.close();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dump));
        exchange.getIn().setBody(bis);
        is = bis;
      }
      is.mark(signature.length);
      Util.readFully(is, signature);
      is.reset();
    } else {
      logger.error("Unexpected message body type: "+body.getClass().getSimpleName());
      exchange.setException(new IllegalArgumentException("Unexpected message body type: "+body.getClass().getSimpleName()));
    }


    logger.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" Signature read: "+bytesToHex(signature));
    if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) {
      exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.GZIP.toString());
      logger.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" GZIP detected file="+filename);
    }else if (compare(signature, new int[]{0x50, 0x4B, 0x03, 0x04})) {
      exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.ZIP.toString());
      logger.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" ZIP detected file="+filename);
    }else if (compare(signature, new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00}) ||
              compare(signature, new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0})) {
      exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.RAR.toString());
      logger.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" RAR detected file="+filename);
    }else if (compare(signature, new int[]{0x37, 0x7a, 0xbc, 0xaf, 0x27, 0x1c})) {
      exchange.getIn().setHeader(MainRouteBuilder.HeaderKeys.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType._7Z.toString());
      logger.debug("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" 7Z detected file="+filename);
    }
    else
      logger.info("["+exchange.getIn().getHeader(MESSAGE_ID_HEADER)+"]"+" No archive detected in file="+filename);
  }

  private static String bytesToHex(byte[] in) {
    final StringBuilder builder = new StringBuilder();
    for(byte b : in) {
      builder.append(String.format("%02x ", b));
    }
    return builder.toString();
  }

  private boolean compare(byte[] signature, int[] sample) {
    for (int i = 0; i < Math.min(signature.length, sample.length); ++i){
      if (signature[i] != (byte)sample[i]) return false;
    }
    return true;
  }

  private boolean equals(byte b, int i) {
    return ((byte)i) == b;
  }
}
