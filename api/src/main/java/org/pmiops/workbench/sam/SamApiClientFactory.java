package org.pmiops.workbench.sam;

import com.google.auth.oauth2.OAuth2Credentials;
import java.io.IOException;
import javax.inject.Provider;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SamApiClientFactory {
  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public SamApiClientFactory(
      FirecloudApiClientFactory firecloudApiClientFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Creates a SAM API client authenticated as the given user via impersonation. This should be used
   * as a last resort, and only when the following conditions hold:
   *
   * <ol>
   *   <li>The API client is used outside the context of an end user request (exception: admin
   *       requests)
   *   <li>RW service account credentials lack sufficient permission to make the desired call
   * </ol>
   *
   * Note that impersonated API calls are subject to the same constraints as end user calls,
   * including access tier membership and eventually Terra ToS enforcement (all Terra requests will
   * fail for accounts that have not completed the latest ToS). TODO(RW-7586): Solve ToS
   * constraints.
   */
  public ApiClient newImpersonatedApiClient(String userEmail) throws IOException {
    OAuth2Credentials delegatedCreds =
        firecloudApiClientFactory.getDelegatedUserCredentials(userEmail);
    ApiClient client = newApiClient();
    client.setAccessToken(delegatedCreds.getAccessToken().getTokenValue());
    return client;
  }

  /**
   * Creates a SAM API client, unauthenticated. Most clients should use an authenticated, request
   * scoped bean instead of calling this directly.
   */
  ApiClient newApiClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    return new ApiClient()
        .setBasePath(workbenchConfig.firecloud.samBaseUrl)
        .setDebugging(workbenchConfig.firecloud.debugEndpoints)
        .addDefaultHeader(
            FirecloudApiClientFactory.X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue)
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds * 1000);
  }
}
