package com.gumirov.shamil.partsib;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.gumirov.shamil.partsib.util.OutputSender;
import com.gumirov.shamil.partsib.util.SessionIdGenerator;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OutputUnitTest extends TestCase {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8084);

  @Test
  public void testOutputSender() throws Exception {
    OutputSender sender = new OutputSender("http://localhost:8084/endpoint");
    sender.setSessionIdGenerator(new SessionIdGenerator() {
      @Override
      public String nextSessionId() {
        return "ID01";
      }
    });
    byte[] b = new byte[]{'0','0','0','0','1','1','1','1'};

    WireMock.stubFor(post(urlEqualTo("/endpoint"))
            .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "text/plain")
              .withBody("")));

    assertTrue(sender.onOutput("1.bin", "pricehookId", b, b.length, 1));
/*    assertTrue(sender.onOutput("2.bin", "pricehookId", b, b.length, 2));
    assertTrue(sender.onOutput("3.bin", "pricehookId", b, b.length, 3));
    assertTrue(sender.onOutput("4.bin", "pricehookId", b, b.length, 7));
    assertTrue(sender.onOutput("5.bin", "pricehookId", b, b.length, 8));
    assertTrue(sender.onOutput("6.bin", "pricehookId", b, b.length, 9));
*/
    WireMock.verify(
        WireMock.postRequestedFor(urlPathEqualTo("/endpoint"))
        .withHeader("X-Filename", equalTo("1.bin"))
        .withHeader("X-Session", equalTo("ID01"))
    );
  }
}
