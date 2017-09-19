package org.pmiops.workbench.config;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheConfig}.
 */
public class WorkbenchConfig {

  public static class FireCloudConfig {
    public static boolean debugging;
  }

  public static FireCloudConfig firecloud;
}
