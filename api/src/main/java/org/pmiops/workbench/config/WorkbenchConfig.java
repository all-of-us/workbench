package org.pmiops.workbench.config;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheConfig}. This should be kept in sync with files in the config/ directory.
 */
public class WorkbenchConfig {

  public static class FireCloudConfig {
    public boolean debugEndpoints;
  }

  public FireCloudConfig firecloud;
}
