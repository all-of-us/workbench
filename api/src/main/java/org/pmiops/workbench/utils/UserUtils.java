package org.pmiops.workbench.utils;

import org.pmiops.workbench.config.WorkbenchConfig;

public class UserUtils {
  public static boolean isUserInDomain(String username, WorkbenchConfig config) {
    return username.endsWith("@" + config.googleDirectoryService.gSuiteDomain);
  }
}
