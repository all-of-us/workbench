package org.pmiops.workbench.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchEnvironment;

public final class ServiceAccounts {

  public static final String FIRECLOUD_ADMIN_CREDS = "firecloudAdminCredentials";
  public static final String GSUITE_ADMIN_CREDS = "gsuiteAdminCredentials";

  /**
   * Retrieves an access token for the Workbench server service account. This
   * should be used carefully, as this account is generally more privileged than
   * an end user researcher account.
   */
  public static String workbenchAccessToken(
      WorkbenchEnvironment workbenchEnvironment, List<String> scopes) throws IOException {
    // When running locally, we get application default credentials in a different way than
    // when running in Cloud.
    if (workbenchEnvironment.isDevelopment()) {
      GoogleCredential credential = GoogleCredential.getApplicationDefault().createScoped(scopes);
      credential.refreshToken();
      return credential.getAccessToken();
    }
    AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
    final AppIdentityService.GetAccessTokenResult accessTokenResult =
        appIdentity.getAccessToken(scopes);
    return accessTokenResult.getAccessToken();
  }
}
