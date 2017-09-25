package org.pmiops.workbench.test;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FileUtils {

  public static String readFileContents(String path) throws Exception {
    InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(path);
    if (in == null) {
      throw new IOException("File not found in classpath: " + path);
    }
    return CharStreams.toString(new InputStreamReader(in, Charset.defaultCharset()));
  }
}
