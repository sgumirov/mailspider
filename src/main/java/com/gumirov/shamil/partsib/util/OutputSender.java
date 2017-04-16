package com.gumirov.shamil.partsib.util;

import org.apache.camel.Consume;
import org.apache.camel.Header;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:20<br/>
 */
public class OutputSender {

  protected Logger log = LoggerFactory.getLogger(getClass());
  
  private String url;

  private SessionIdGenerator sessionIdGenerator = new SessionIdGenerator() {
    @Override
    public String nextSessionId() {
      return UUID.randomUUID().toString();
    }
  };

  public void setUrl(String url) {
    this.url = url;
  }

  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
    this.sessionIdGenerator = sessionIdGenerator;
  }

  public OutputSender(String url){
    this.url = url;
  }
  
  public boolean onOutput(String filename, String priceHookId, byte[] file, int length, int maxLength) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    //split
    int part = 0;
    int totalParts = length / maxLength;
    if (length % maxLength != 0) ++totalParts;
    String uuid = sessionIdGenerator.nextSessionId();
    try {
      byte[] b;
      if (totalParts == 1) b = file;
      else b = new byte[length > maxLength ? maxLength : length];
      while (part < totalParts) {
        int len = (totalParts == 1 ? length : (part < totalParts-1 ? maxLength : length % maxLength));
        System.arraycopy(file, part*maxLength, b, 0, len);
        HttpPost httppost = new HttpPost(url);
        ByteArrayEntity reqEntity = new ByteArrayEntity(b, 0, len, ContentType.APPLICATION_OCTET_STREAM);

        if (filename != null) httppost.setHeader("X-Filename", Base64.getEncoder().encodeToString(filename.getBytes("UTF8")));
        if (priceHookId != null) httppost.setHeader("X-Pricehook", priceHookId);
        if (totalParts > 1) httppost.setHeader("X-Part", ""+part);
        if (totalParts > 1) httppost.setHeader("X-Parts-Total", ""+totalParts);
        httppost.setHeader("X-Session", uuid.toString());
        reqEntity.setChunked(true);
        httppost.setEntity(reqEntity);

        System.out.println("Executing request: " + httppost.getRequestLine());
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
          System.out.println("----------------------------------------");
          System.out.println(response.getStatusLine());
          System.out.println(EntityUtils.toString(response.getEntity()));
        } finally {
          response.close();
        }
        ++part;
      }
    } catch (FileNotFoundException e) {
      log.info("[OutputSenderEndpoint] Error: cannot find file to send: "+filename, e);
      return false;
    } catch (IOException e) {
      log.info("[OutputSenderEndpoint] IOError: cannot send file: "+filename, e);
      return false;
    } finally {
      httpclient.close();
    }
    return true;
  }
}
