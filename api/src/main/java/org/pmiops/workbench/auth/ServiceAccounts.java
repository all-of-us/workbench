package org.pmiops.workbench.auth;

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
   * Retrieves an access token for the Workbench server service account. This should be used
   * carefully, as this account is generally more privileged than an end user researcher account.
   */
  public static String workbenchAccessToken(List<String> scopes) throws IOException {
    GoogleCredentials scopedCreds = GoogleCredentials.getApplicationDefault().createScoped(scopes);
    scopedCreds.refresh();
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
