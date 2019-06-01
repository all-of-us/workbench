package org.pmiops.workbench.auth;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles functionality related to loading service account credentials and generating derived /
 * impersonated credentials.
 */
@Component
public class ServiceAccounts {

  private final HttpTransport httpTransport;

  @Autowired
  public ServiceAccounts(HttpTransport httpTransport) {
    this.httpTransport = httpTransport;
  }

  public GoogleCredential.Builder getCredentialBuilder() {
    return new GoogleCredential.Builder();
  }

  /**
   * Retrieves an access token for the Workbench server service account. This should be used
   * carefully, as this account is generally more privileged than an end user researcher account.
   */
  public String workbenchAccessToken(WorkbenchEnvironment workbenchEnvironment, List<String> scopes)
      throws IOException {
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

  /**
   * Converts a service account Google credential into credentials for impersonating an end user.
   * This method assumes that the given service account has been enabled for domain-wide delegation,
   * and the given set of scopes have been included in the GSuite admin panel.
   *
   * <p>See docs/domain-delegation.md for more details.
   *
   * @param serviceAccountCredential
   * @param userEmail Email address of the user to impersonate.
   * @param scopes The list of Google / OAuth API scopes to be authorized for.
   * @return
   * @throws IOException
   */
  public GoogleCredential getImpersonatedCredential(
      GoogleCredential serviceAccountCredential, String userEmail, List<String> scopes)
      throws IOException {
    // Build derived credentials for impersonating the target user.
    GoogleCredential impersonatedUserCredential =
        getCredentialBuilder()
            .setJsonFactory(getDefaultJsonFactory())
            .setTransport(httpTransport)
            .setServiceAccountUser(userEmail)
            .setServiceAccountId(serviceAccountCredential.getServiceAccountId())
            .setServiceAccountScopes(scopes)
            .setServiceAccountPrivateKey(serviceAccountCredential.getServiceAccountPrivateKey())
            .setServiceAccountPrivateKeyId(serviceAccountCredential.getServiceAccountPrivateKeyId())
            .setTokenServerEncodedUrl(serviceAccountCredential.getTokenServerEncodedUrl())
            .build();

    // Initiate the OAuth flow to populate the access token.
    impersonatedUserCredential.refreshToken();
    return impersonatedUserCredential;
  }
}
