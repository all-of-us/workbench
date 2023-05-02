package org.pmiops.workbench.auth;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
@Component
public class ServiceAccounts {

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
  public static GoogleCredentials getScopedServiceCredentials(List<String> scopes)
      throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault().createScoped(scopes);
    credentials.refreshIfExpired();
    return credentials;
  }

  /**
   * Retrieves an access token with the specified set of scopes derived from Workbench service
   * credentials.
   */
  public static String getScopedServiceAccessToken(List<String> scopes) throws IOException {
    return getScopedServiceCredentials(scopes).getAccessToken().getTokenValue();
  }

  public static String getServiceAccountEmail(String serviceAccountName, String projectId) {
    return String.format("%s@%s.iam.gserviceaccount.com", serviceAccountName, projectId);
  }
}
