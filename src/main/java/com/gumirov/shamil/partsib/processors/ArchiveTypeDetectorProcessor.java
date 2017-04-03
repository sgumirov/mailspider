package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.Util;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 22:52<br/>
 */
public class ArchiveTypeDetectorProcessor implements Processor {
  static Logger logger = LoggerFactory.getLogger(ArchiveTypeDetectorProcessor.class);

  @Override
  public void process(Exchange exchange) throws Exception {
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
    InputStream fis = exchange.getIn().getBody(InputStream.class);

    byte [] signature = new byte[8];
    Util.readFully(fis, signature);
    //gzip:     1f 8b
    //rar >1.5: 52 61 72 21 1A 07 00
    //rar >5.0: 52 61 72 21 1A 07 01 00
    //zip:      50 4B 03 04
    logger.info("Signature read: "+bytesToHex(signature));
    if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) {
      exchange.getIn().setHeader(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.GZIP.toString());
      logger.info("GZIP detected file="+filename);
    }else if (compare(signature, new int[]{0x50, 0x4B, 03, 04})) {
      exchange.getIn().setHeader(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.ZIP.toString());
      logger.info("ZIP detected file="+filename);
    }else if (compare(signature, new int[]{0x52, 0x61, 0x72, 0x21,0x1A, 07, 00}) ||
             compare(signature, new int[]{0x52, 0x61, 0x72, 0x21,0x1A, 07, 01, 0})) {
      exchange.getIn().setHeader(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType.RAR.toString());
      logger.info("RAR detected file="+filename);
    }else if (compare(signature, new int[]{0x37, 0x7a, 0xbc, 0xaf, 0x27, 0x1c})) {
      exchange.getIn().setHeader(MainRouteBuilder.COMPRESSED_TYPE_HEADER_NAME, MainRouteBuilder.CompressorType._7Z.toString());
      logger.info("7Z detected file="+filename);
    }
    else
      logger.info("No archive detected file="+filename);
  }

  private static String bytesToHex(byte[] in) {
    final StringBuilder builder = new StringBuilder();
    for(byte b : in) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }

  private boolean compare(byte[] signature, int[] sample) {
    for (int i = 0; i < Math.min(signature.length, sample.length); ++i){
      if (signature[i] != sample[i]) return false;
    }
    return true;
  }
}
