package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.NamedByteArray;
import com.gumirov.shamil.partsib.util.NamedResource;
import com.gumirov.shamil.partsib.util.NamedStream;
import com.gumirov.shamil.partsib.util.Unpacker;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits message on archive contents. For each extracted from archive file it copies original message with single
 * attachment file.
 */
public class UnpackerSplitter {
  static Logger log = LoggerFactory.getLogger(UnpackerSplitter.class);
  private Unpacker unpacker;

  public UnpackerSplitter(Unpacker unpacker) {
    this.unpacker = unpacker;
  }

  //IOC for unit-testing
  public void setUnpacker(Unpacker unpacker) {
    this.unpacker = unpacker;
  }

  public List<Message> unpack(Exchange exchange) {
    String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
    byte[] bytes = exchange.getIn().getBody(byte[].class);
    List<Message> outMsgs = new ArrayList<>();
    try {
      List<NamedResource> files = unpacker.unpack(bytes);
      for (NamedResource nr : files) {
        Message copy = exchange.getIn().copy();
        if (nr instanceof NamedStream)
          copy.setBody(((NamedStream)nr).getStream(), InputStream.class);
        else if (nr instanceof NamedByteArray)
          copy.setBody(((NamedByteArray)nr).getBytes(), byte[].class);
        else
          throw new RuntimeException("Unexpected body type: "+nr.getClass().getSimpleName());
        copy.setHeader(Exchange.FILE_NAME, nr.getName());
        copy.setHeader(MainRouteBuilder.LENGTH_HEADER, nr.getLength());
        outMsgs.add(copy);
      }
    }catch (Exception e){
      log.error("Error while unpacking archive: "+filename+String.format(", will return %d extracted files", outMsgs.size()), e);
    }
    return outMsgs;
  }
}


