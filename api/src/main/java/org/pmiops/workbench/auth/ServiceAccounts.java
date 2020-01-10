package org.pmiops.workbench.auth;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.auth.appengine.AppEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.List;

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
public class ServiceAccounts {

  private static final String SIGN_JWT_URL_FORMAT =
      "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:signJwt";

  /**
   * Returns an appropriate set of backend service credentials with the given set of scopes.
   *
   * <p>This method uses AppIdentityService to return an instance of scoped AppEngineCredentials
   * when running in an App Engine environment.
   *
   * <p>Unfortunately, if we use GoogleCredentials.getApplicationDefault() from within an App Engine
   * environment, the returned credentials will be an instance of ComputeEngineCredentials, which
   * doesn't support scoped access tokens. Frustratingly, the call to .createScoped will silently
   * proceed by doing nothing -- meaning we only learn about the error once an attempt to use these
   * credentials fails in a downstream service due to bad scopes.
   *
   * <p>See https://github.com/googleapis/google-auth-library-java/issues/272 and
   * https://github.com/googleapis/google-auth-library-java/issues/172 for reference; this seems to
   * be a common pain point for users of the com.google.auth.oauth2 library.
   *
   * @param scopes
   * @return
   * @throws IOException
   */
  private static GoogleCredentials getScopedServiceCredentials(List<String> scopes)
      throws IOException {

    if (SystemProperty.environment.value().equals(SystemProperty.Environment.Value.Development)) {
      // When running in a local dev environment, we simply get the application default credentials.
      //
      // TODO(gjuggler): it may be possible to remove this branch point altogether, and use the
      // AppIdentityService approach even when running a local app engine server. I tested this
      // out locally and it *seemed* to work, but it needs a bit more careful vetting.
      return GoogleCredentials.getApplicationDefault().createScoped(scopes);
    } else {
      AppIdentityService appIdentityService = AppIdentityServiceFactory.getAppIdentityService();
      return AppEngineCredentials.newBuilder()
          .setScopes(scopes)
          .setAppIdentityService(appIdentityService)
          .build();
    }
  }

  /**
   * Retrieves an access token with the specified set of scopes derived from Workbench service
   * credentials.
   */
  public static String getScopedServiceAccessToken(List<String> scopes) throws IOException {
    GoogleCredentials scopedCreds = getScopedServiceCredentials(scopes);
    scopedCreds.refreshIfExpired();
    return scopedCreds.getAccessToken().getTokenValue();
  }

  /**
   * Converts a service account Google credential into credentials for impersonating an end user.
   * This method assumes that the given service account has been enabled for domain-wide delegation,
   * and the given set of scopes have been included in the GSuite admin panel.
   *
   * <p>See docs/domain-delegation.md for more details.
   *
   * @param originalCredentials
   * @param userEmail Email address of the user to impersonate.
   * @param scopes The list of Google / OAuth API scopes to be authorized for.
   * @return
   * @throws IOException
   */
  public static GoogleCredentials getImpersonatedCredentials(
      GoogleCredentials originalCredentials, String userEmail, List<String> scopes)
      throws IOException {
    GoogleCredentials impersonatedCreds =
        originalCredentials.createScoped(scopes).createDelegated(userEmail);
    impersonatedCreds.refresh();
    return impersonatedCreds;
  }
}
