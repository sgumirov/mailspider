package com.gumirov.shamil.partsib;

import com.gumirov.shamil.partsib.util.Util;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class ReadFullyUnitTest {
  @Test
  public void test() throws IOException {
    String s = "hello world!";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10; i++) sb.append(s);
    byte[] b = sb.toString().getBytes();
    File tmpFile = File.createTempFile("123", null);
    FileOutputStream fos = new FileOutputStream(tmpFile);
    fos.write(b);
    fos.flush();
    fos.close();
    FileInputStream fis = new FileInputStream(tmpFile);
    byte[] in = new byte[b.length];
    int read = Util.readFully(fis, in);
    Assert.assertEquals(b.length, read);
    fis.close();
  }
}
