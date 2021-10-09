package org.pmiops.workbench.firecloud;

import com.google.api.client.http.HttpTransport;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FirecloudApiClientFactory {
  public static final String X_APP_ID_HEADER = "X-App-ID";

  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "firecloud-admin";
  // The set of Google OAuth scopes required for access to FireCloud APIs. If FireCloud ever changes
  // its API scopes (see https://api.firecloud.org/api-docs.yaml), we'll need to update this list.
  public static final List<String> SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");

  private final IamCredentialsClient iamCredentialsClient;
  private final HttpTransport httpTransport;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public FirecloudApiClientFactory(
      IamCredentialsClient iamCredentialsClient,
      HttpTransport httpTransport,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.iamCredentialsClient = iamCredentialsClient;
    this.httpTransport = httpTransport;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Given an email address of an AoU user, generates {@link OAuth2Credentials} suitable for
   * accessing data on behalf of that user.
   *
   * <p>This relies on domain-wide delegation of authority in Google's OAuth flow; see
   * /api/docs/domain-wide-delegation.md for more details.
   */
  public OAuth2Credentials getDelegatedUserCredentials(String userEmail) throws IOException {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    final OAuth2Credentials delegatedCreds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail(
                ADMIN_SERVICE_ACCOUNT_NAME, workbenchConfig.server.projectId),
            userEmail,
            SCOPES,
            iamCredentialsClient,
            httpTransport);
    delegatedCreds.refreshIfExpired();
    return delegatedCreds;
  }

  public ApiClient newImpersonatedApiClient(String userEmail) throws IOException {
    OAuth2Credentials delegatedCreds = getDelegatedUserCredentials(userEmail);
    ApiClient client = newApiClient();
    client.setAccessToken(delegatedCreds.getAccessToken().getTokenValue());
    return client;
  }

  public ApiClient newApiClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.firecloud.baseUrl);
    apiClient.addDefaultHeader(X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }
}
