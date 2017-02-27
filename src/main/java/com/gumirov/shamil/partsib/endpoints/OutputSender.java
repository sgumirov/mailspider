package com.gumirov.shamil.partsib.endpoints;

import org.apache.camel.Consume;
import org.apache.camel.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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
  
  public void onOutput(String filename, InputStream is) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    
    try {
      HttpPost httppost = new HttpPost(url);
      InputStreamEntity reqEntity = new InputStreamEntity(is, -1, ContentType.APPLICATION_OCTET_STREAM);
      httppost.setHeader("X-Filename", filename);
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
    } catch (FileNotFoundException e) {
      log.info("[OutputSenderEndpoint] Error: cannot find file to send: "+filename, e);
    } catch (IOException e) {
      log.info("[OutputSenderEndpoint] IOError: cannot send file: "+filename, e);
    } finally {
      httpclient.close();
    }
  }
}
