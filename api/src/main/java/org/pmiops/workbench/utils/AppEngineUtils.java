package org.pmiops.workbench.utils;

public class AppEngineUtils {
  public static boolean IS_GAE =
      System.getProperty("com.google.appengine.runtime.version") != null
          && !System.getProperty("com.google.appengine.runtime.version").startsWith("dev");
}
