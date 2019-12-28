package org.pmiops.workbench.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.springframework.stereotype.Component;

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
@Component
public class ServiceAccounts {

  /**
   * Retrieves an access token for the Workbench server service account. This should be used
   * carefully, as this account is generally more privileged than an end user researcher account.
   */
  public String workbenchAccessToken(WorkbenchEnvironment workbenchEnvironment, List<String> scopes)
      throws IOException {
    return GoogleCredentials.getApplicationDefault()
        .createScoped(scopes)
        .refreshAccessToken()
        .getTokenValue();
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
  public GoogleCredentials getImpersonatedCredential(
      ServiceAccountCredentials originalCredentials, String userEmail, List<String> scopes)
      throws IOException {
    GoogleCredentials delegatedCreds =
        originalCredentials.createScoped(scopes).createDelegated(userEmail);
    delegatedCreds.refreshAccessToken();
    return delegatedCreds;
  }
}
