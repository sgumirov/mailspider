package com.gumirov.shamil.partsib.util;

import org.apache.camel.Consume;
import org.apache.camel.Header;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
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

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:20<br/>
 */
public class HttpPostFileSender {

  protected Logger log = LoggerFactory.getLogger(getClass());
  
  private String url;

  public HttpPostFileSender(String url){
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
        httppost.setHeader("X-Session", uuid);
        reqEntity.setChunked(true);
        httppost.setEntity(reqEntity);

        log.info(format("Executing request X-Filename='%s' (part %d/%d; X-Part = %d; Length = %d): %s", filename!=null?filename:"null", part+1, totalParts, part, reqEntity.getContentLength(), httppost.getRequestLine()));
        try (CloseableHttpResponse response = httpclient.execute(httppost)) {
          log.info(format("Http response (part %d/%d): %s", part+1, totalParts, response.getStatusLine().toString()));
          log.debug(EntityUtils.toString(response.getEntity()));
          if (response.getStatusLine().getStatusCode() != HTTP_OK) {
            return false;
          }
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
  private SessionIdGenerator sessionIdGenerator = () -> format("%09d", r.nextInt(Integer.MAX_VALUE));

  public void setUrl(String url) {
    this.url = url;
  }

  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
    this.sessionIdGenerator = sessionIdGenerator;
  }
}
