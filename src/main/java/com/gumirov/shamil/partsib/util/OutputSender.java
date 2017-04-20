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
import java.security.SecureRandom;
import java.util.*;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:20<br/>
 */
public class OutputSender {

  protected Logger log = LoggerFactory.getLogger(getClass());
  
  private String url;

  public OutputSender(String url){
    this.url = url;
  }

  private Random r = new Random(System.currentTimeMillis());

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
        httppost.setHeader("X-Part", ""+part);
        httppost.setHeader("X-Parts-Total", ""+totalParts);
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
      return true;
    } catch (FileNotFoundException e) {
      log.error("[OutputSenderEndpoint] Error: cannot find file to send: "+filename, e);
    } catch (IOException e) {
      log.error("[OutputSenderEndpoint] IOError: cannot send file: "+filename, e);
    } finally {
      httpclient.close();
    }
    return false;
  }
  
  /**
   * Default implementation:
   */
  private SessionIdGenerator sessionIdGenerator = () -> String.format("%09d", r.nextInt(Integer.MAX_VALUE));

  public void setUrl(String url) {
    this.url = url;
  }

  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
    this.sessionIdGenerator = sessionIdGenerator;
  }
}
