package org.pmiops.workbench.auth;

public class Constants {
  /**
   * Bean names for the various types of GoogleCredential dependencies that may be injected into
   * workbench classes. These should be used in bean definitions (in Config classes) and
   * in @Qualifier annotations (in service and component classes).s
   */
  public static final String CLOUD_RESOURCE_MANAGER_ADMIN_CREDS =
      "cloudResourceManagerAdminCredentials";

  public static final String FIRECLOUD_ADMIN_CREDS = "firecloudAdminCredentials";

  public static final String GSUITE_ADMIN_CREDS = "gsuiteAdminCredentials";
  public static final String DEFAULT_SERVICE_ACCOUNT_CREDS = "defaultServiceAccountCredentials";
}
