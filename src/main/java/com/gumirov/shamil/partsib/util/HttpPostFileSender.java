package com.gumirov.shamil.partsib.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * HTTP-related stuff is here.
 * Note: deprecation of onOutput() since we moved MailSpider from byte[] to streams for content but some tests yet use the byte[].
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 4/1/2017 Time: 23:20<br/>
 */
public class HttpPostFileSender {

  protected Logger log = LoggerFactory.getLogger(getClass());
  private String url;
  private Random r = new Random(System.currentTimeMillis());
  /**
   * Default implementation of session id
   */
  private SessionIdGenerator sessionIdGenerator = () -> format("%09d", r.nextInt(Integer.MAX_VALUE));

  public HttpPostFileSender(String url){
    this.url = url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Setter for AT
   * @param sessionIdGenerator generator for session id for automated testing
   */
  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
    this.sessionIdGenerator = sessionIdGenerator;
  }

  /**
   * Split file to parts and POST to HTTP endpoint.
   * @param partLen max part length. Used to split file into parts
   * @param instanceId current Mailspider instance id (currently from config), can be null
   * @return false in case non-200 code or
   * @deprecated use stream-based variant {@link #send(String, String, InputStream, int, int, String, String, String)}
   */
  @Deprecated
  public boolean onOutput(String filename, String priceHookId, byte[] file, int length, int partLen, String mid,
                          String instanceId, String sourceEndpoint) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    //split
    int part = 0;
    int totalParts = length / partLen;
    if (length % partLen != 0) ++totalParts;
    String uuid = sessionIdGenerator.nextSessionId();
    try {
      byte[] b;
      if (totalParts == 1) b = file;
      else b = new byte[length > partLen ? partLen : length];
      while (part < totalParts) {
        int len = (totalParts == 1 ? length : (part < totalParts-1 ? partLen :
                   length % partLen == 0 ? partLen : length % partLen));
        System.arraycopy(file, part*partLen, b, 0, len);
        sendPart(filename, priceHookId, b, len, part, totalParts, uuid, httpclient, mid, instanceId, sourceEndpoint);
        ++part;
      }
      return true;
    } catch (IllegalStateException e) {
      return false;
    } catch (FileNotFoundException e) {
      log.error("[OutputSenderEndpoint] Error: cannot find file to send: "+filename, e);
    } catch (IOException e) {
      log.error("[OutputSenderEndpoint] IOError: cannot send file: "+filename, e);
    } finally {
      httpclient.close();
    }
    return false;
  }

  public boolean send(String filename, String pricehookId, InputStream is, int length, int partLen, String mid,
                      String instanceId, String sourceEndpoint) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    //split
    int part = 0;
    int totalParts = length / partLen;
    if (length % partLen != 0) ++totalParts;
    String uuid = sessionIdGenerator.nextSessionId();
    try {
      byte[] b;
      if (totalParts == 1) b = Util.readFully(is);
      else b = new byte[length > partLen ? partLen : length];
      while (part < totalParts) {
        int len = (totalParts == 1 ? length : (part < totalParts-1 ? partLen :
            length % partLen == 0 ? partLen : length % partLen));
        Util.readFully(is, b);
        sendPart(filename, pricehookId, b, len, part, totalParts, uuid, httpclient, mid, instanceId, sourceEndpoint);
        ++part;
      }
      return true;
    } catch (IllegalStateException e) {
      return false;
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
   * Sends single part.
   * @param filename name of file
   * @param priceHookId tag id
   * @param b bytes to send
   * @param len length of content in byte[]
   * @param part current part #, from 0
   * @param totalParts how many parts in total
   * @param uuid unique id of current sending session (thus would be different for different trying to send same file).
   * @param httpclient {@link CloseableHttpClient} to be used
   * @throws IOException if IOException happends
   * @throws IllegalStateException when API endpoint returned anything else than 200 OK response
   */
  private void sendPart(String filename, String priceHookId, byte[] b, int len, int part, int totalParts, String uuid,
                        CloseableHttpClient httpclient, String mid, String instanceId, String sourceEndpointId) throws IOException, IllegalStateException {
    HttpPost httppost = new HttpPost(url);
    ByteArrayEntity reqEntity = new ByteArrayEntity(b, 0, len, ContentType.APPLICATION_OCTET_STREAM);

    log.info("["+mid+"] HTTP Sending file="+filename+" pricehookId="+priceHookId+" part="+(1+part)+"/"+totalParts+" length="+len);

    if (filename != null) httppost.setHeader("X-Filename", Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8)));
    if (priceHookId != null) httppost.setHeader("X-Pricehook", priceHookId);
    httppost.setHeader("X-Part", ""+part);
    httppost.setHeader("X-Parts-Total", ""+totalParts);
    httppost.setHeader("X-Session", uuid);
    if (instanceId != null) httppost.setHeader("X-Instance-Id", instanceId);
    if (sourceEndpointId != null) httppost.setHeader("X-Source-Endpoint-Id", sourceEndpointId);
    reqEntity.setChunked(true);
    httppost.setEntity(reqEntity);

    try (CloseableHttpResponse response = httpclient.execute(httppost)) {
      log.info(format("["+mid+"] Http response (part %d/%d): %s", part+1, totalParts, response.getStatusLine().toString()));
      if (response.getStatusLine().getStatusCode() != HTTP_OK) {
        throw new IllegalStateException();
      }
    }
  }
}
