package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.sun.mail.util.MimeUtil;
import org.junit.Test;

import javax.mail.MessagingException;

import java.io.*;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * @author: Shamil@Gumirov.com
 * Copyright (c) 2017 by Shamil Gumirov.
 */
public class EmailFileExtractTest extends EmailRouteAT {
  @Test
  public void testFileExtract() throws Exception {
    WireMock.reset();
    execute(() -> {
          try {
            prepareHttpdOK();
            sendEml(getClass().getClassLoader().getResourceAsStream("message.eml"));
          } catch (MessagingException e) {
            e.printStackTrace();
          }
        },
        20000,
        () -> {
          verify(1, postRequestedFor(urlEqualTo(httpendpoint)));
          LoggedRequest req = WireMock.findAll(postRequestedFor(urlEqualTo(httpendpoint))).get(0);
          byte[] file = req.getBody();
          try {
            String fname = new String(Base64.getDecoder().decode(req.getHeader("X-Filename")), "UTF-8");
            FileOutputStream fs = new FileOutputStream(fname);
            log.info("Writing file: "+fname);
            fs.write(file);
            fs.flush();
            fs.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
    );
  }
}
