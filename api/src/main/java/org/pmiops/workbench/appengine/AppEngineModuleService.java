package org.pmiops.workbench.appengine;

import com.google.appengine.api.modules.ModulesService;

/**
 * Offers similar functionally as {@link com.google.appengine.api.modules.ModulesService} in Java 11
 * world. See https://cloud.google.com/appengine/docs/standard/java11/java-differences#modules.
 */
public class AppEngineModuleService {
  /** See {@link ModulesService#getCurrentInstanceId()}. */
  public String getCurrentInstanceId() {
    return System.getenv("GAE_INSTANCE");
  }
}
