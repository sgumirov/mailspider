package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.OutputSender;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

public class OutputUnitTest extends TestCase {
  public void testOutputSender() throws Exception {
    OutputSender sender = new OutputSender("http://im.mad.gd/2.php");
    byte[] b = new byte[]{'0','0','0','0','1','1','1','1'};
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 1));
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 2));
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 3));
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 7));
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 8));
    assertTrue(sender.onOutput("test.bin", "pricehookId", b, b.length, 9));
  }
}
