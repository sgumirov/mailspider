package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.mail.MailBindingFixNestedAttachments;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailBinding;
import org.apache.camel.component.mail.MailEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.*;
import org.junit.runners.MethodSorters;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;

//Covered by StutzenATest
@Ignore
public class SubjectFixCamelMailBindingUnitTest extends CamelTestSupport {
  final String username = "partsib", pwd = username;
  final String url = "imap://"+ username +"@localhost:3143?password="+pwd+"&consumer.delay=100&delete=true&mapMailMessage=false&mimeDecodeHeaders=true";
  final String urlNoDecode = "imap://"+ username +"@127.0.0.1:3143?password="+pwd+"&consumer.delay=200&delete=true&mapMailMessage=false";
  final String expectedSubj = "Прайс-лист от www.stutzen.ru Склад Новосибирск";
  final String emailRaw = "stutzen/stutzen-nsk.txt";

  @Rule
  public GreenMailRule greenMail =  new GreenMailRule(ServerSetupTest.IMAP);

  private String mockUrl = "mock://mail";
  private String mockUrlNoDecode = "mock://mailNoDecode";

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        MailBinding mailBinding = new MailBindingFixNestedAttachments();

        MailEndpoint mailEndpoint = getContext().getEndpoint(url, MailEndpoint.class);
        mailEndpoint.setBinding(mailBinding);
        from(mailEndpoint).to(mockUrl).routeId("CORRECT");

        MailEndpoint mailEndpoint2 = getContext().getEndpoint(urlNoDecode, MailEndpoint.class);
        mailEndpoint2.setBinding(mailBinding);
        from(mailEndpoint2).to(mockUrlNoDecode).routeId("INCCORRECT");
      }
    };
  }

  @Before
  public void cleanGreenMail() throws FileNotFoundException {
    greenMail.reset();
    GreenMailUser user = greenMail.setUser(username, pwd);
    user.deliver(GreenMailUtil.newMimeMessage(getClass().getClassLoader().getResourceAsStream(emailRaw)));
  }

  @Test
  public void test() throws MessagingException, InterruptedException {
    assertTrue(greenMail.getReceivedMessages().length > 0);

    MockEndpoint mock = getMockEndpoint(mockUrl);
    mock.expectedMessageCount(1);
    mock.expectedBodyReceived().body().isNotNull();
    assertMockEndpointsSatisfied();

    Exchange exchange = getMockEndpoint(mockUrl).getReceivedExchanges().get(0);
    org.apache.camel.Message javaMailMessage = exchange.getIn();
    assertNotNull("message must be decoded", javaMailMessage);
    assertEquals("Subject decoded incorrectly", "Прайс-лист от www.stutzen.ru Склад Новосибирск",
        javaMailMessage.getHeader("Subject"));
  }

  @Test
  public void testNoDecode() throws MessagingException, InterruptedException {
    assertTrue(greenMail.getReceivedMessages().length > 0);

    MockEndpoint mock = getMockEndpoint(mockUrlNoDecode);
    mock.expectedMessageCount(1);
    mock.expectedBodyReceived().body().isNotNull();
    assertMockEndpointsSatisfied();

    Exchange exchange = getMockEndpoint(mockUrlNoDecode).getReceivedExchanges().get(0);
    Message javaMailMessage = exchange.getIn();
    assertNotNull("message must be decoded", javaMailMessage);
    assertNotEquals("Subject should be decoded incorrectly:\n" +
            "(actual="+javaMailMessage.getHeader("Subject")+",\n" +
            "expected="+expectedSubj+").\n" +
            "If this tests started to fail unexpectedly there's " +
        "high chance that javamail is now fixed and no need for out fix",
        expectedSubj,
        javaMailMessage.getHeader("Subject"));
  }
}
