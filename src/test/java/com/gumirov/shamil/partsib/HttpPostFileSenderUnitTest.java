package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.HttpPostFileSender;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpPostFileSenderUnitTest {
  private final int PART_LEN = 7;
  private final List<TestCase> testCases = Arrays.asList(
      //todo check length 0
      new TestCase(1, new int[]{1}),
      new TestCase(2, new int[]{2}),
      new TestCase(5, new int[]{5}),
      new TestCase(6, new int[]{6}),
      new TestCase(7, new int[]{7}),
      new TestCase(8, new int[]{7,1}),
      new TestCase(9, new int[]{7,2}),
      new TestCase(13, new int[]{7,6}),
      new TestCase(14, new int[]{7,7}),
      new TestCase(15, new int[]{7,7,1}),
      new TestCase(30, new int[]{7,7,7,7,2})
  );

  @Test
  public void test() throws IOException {
    for (TestCase test : testCases) {
      test(test);
    }
  }

  private void test(TestCase test) throws IOException {
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
    CloseableHttpClient dummyClient = mock(CloseableHttpClient.class);
    when(dummyClient.execute(any())).thenReturn(response);
    TestableHttpPostFileSender sender = spy(new TestableHttpPostFileSender("", dummyClient));
    sender.send("", "", makeIs(test.fileLength), test.fileLength, PART_LEN, "", "", "");
    for (int i = 0; i < test.partLengths.length; ++i) {
      int len = test.partLengths[i];
      verify(sender).sendPart(any(), any(), any(byte[].class), eq(len), eq(i), eq(test.partLengths.length), any(), any(), any(), any(), any());
    }
  }

  private InputStream makeIs(int len) {
    byte[] b = new byte[len];
    for (int i = 0; i < len; ++i) b[i] = 'a';
    return new ByteArrayInputStream(b);
  }

  private static class TestCase{
    int fileLength;
    int[] partLengths;
    TestCase(int fileLength, int[] partLengths) {
      this.fileLength = fileLength;
      this.partLengths = partLengths;
    }
  }

  class TestableHttpPostFileSender extends HttpPostFileSender {
    private CloseableHttpClient client;

    public TestableHttpPostFileSender(String url, CloseableHttpClient client) {
      super(url);
      this.client = client;
    }

    @Override
    protected CloseableHttpClient getHttpClient() {
      return client;
    }
  }
}

