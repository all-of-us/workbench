package org.pmiops.workbench.tools;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;

public class ImpersonatedServiceAccountApiClientFactory extends ApiClientFactory {

  public ImpersonatedServiceAccountApiClientFactory(
      String targetServiceAccount, WorkbenchConfig config) {
    try {
      this.apiClient = newApiClient(targetServiceAccount, config);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ApiClient newApiClient(String targetServiceAccount, WorkbenchConfig config)
      throws IOException {
    IamCredentialsClient iamCredentialsClient = IamCredentialsClient.create();
    List<String> delegates =
        Arrays.asList("projects/-/serviceAccounts/" + config.auth.serviceAccountApiUsers.get(0));

    String accessToken =
        iamCredentialsClient
            .generateAccessToken(
                "projects/-/serviceAccounts/" + targetServiceAccount,
                delegates,
                Arrays.asList(FC_SCOPES),
                Duration.newBuilder().setSeconds(60 * 60).build())
            .getAccessToken();

    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(config.firecloud.baseUrl);
    apiClient.setAccessToken(accessToken);

    return apiClient;
  }
}
