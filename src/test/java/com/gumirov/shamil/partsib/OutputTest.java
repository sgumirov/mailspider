package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.gumirov.shamil.partsib.util.OutputSender;
import com.gumirov.shamil.partsib.util.SessionIdGenerator;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.TestCase.assertTrue;


/**
 * This is AT for OutputSender with mock HTTPD. This is a real simulation of HTTP req/res.
 * TODO Add error response codes and see Camel session retries.
 */
public class OutputTest {
  final int port = 8888;
  
  
  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(WireMockConfiguration.wireMockConfig().port(port));
  
  @Before
  public void before(){
    stubFor(post(urlEqualTo("/endpoint"))
        .willReturn(aResponse()
            .withStatus(200)));
  }
  
  public Runnable expect(String filename, int parts, String pricehookId) {
    return () -> {
      try {
        for (int i = 0; i < parts; ++i) {
          WireMock.verify(
              WireMock.postRequestedFor(urlPathEqualTo("/endpoint"))
                  .withHeader("X-Filename", equalTo(java.util.Base64.getEncoder().encodeToString(filename.getBytes("UTF-8"))))
                  .withHeader("X-Session", equalTo(pricehookId))
                  .withHeader("X-Part", equalTo(""+i))
                  .withHeader("X-Parts-Total", equalTo(""+parts))
          );
        }
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    };
  }
  
  @Test
  public void testOutputSender() throws Exception {
    OutputSender sender = new OutputSender("http://127.0.0.1:"+port+"/endpoint");
    sender.setSessionIdGenerator(new SessionIdGenerator() {
      @Override
      public String nextSessionId() {
        return "ID01";
      }
    });
    byte[] b = new byte[]{'0','0','0','0','1','1','1','1'};

    assertTrue(sender.onOutput("1.bin", "pricehookId", b, b.length, 1)); //8
    assertTrue(sender.onOutput("2.bin", "pricehookId", b, b.length, 2)); //4
    assertTrue(sender.onOutput("3.bin", "pricehookId", b, b.length, 3)); //3
    assertTrue(sender.onOutput("4.bin", "pricehookId", b, b.length, 7)); //2
    assertTrue(sender.onOutput("5.bin", "pricehookId", b, b.length, 8)); //1
    assertTrue(sender.onOutput("6.bin", "pricehookId", b, b.length, 9)); //1

    expect("1.bin", 8, "ID01").run();
    expect("2.bin", 4, "ID01").run();
    expect("3.bin", 3, "ID01").run();
    expect("4.bin", 2, "ID01").run();
    expect("5.bin", 1, "ID01").run();
    expect("6.bin", 1, "ID01").run();
  }
}
