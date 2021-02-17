package org.pmiops.workbench.tools;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.pmiops.workbench.firecloud.ApiClient;

public class ImpersonatedServiceAccountApiClientFactory extends ApiClientFactory {

  public ImpersonatedServiceAccountApiClientFactory(String targetServiceAccount, String fcBaseUrl)
      throws IOException {
    super(newApiClient(targetServiceAccount, fcBaseUrl));
  }

  public static String getAccessToken(String targetServiceAccount) throws IOException {
    IamCredentialsClient iamCredentialsClient = IamCredentialsClient.create();

    String accessToken =
        iamCredentialsClient
            .generateAccessToken(
                "projects/-/serviceAccounts/" + targetServiceAccount,
                Collections.EMPTY_LIST,
                Arrays.asList(FC_SCOPES),
                Duration.newBuilder().setSeconds(60 * 60).build())
            .getAccessToken();

    return accessToken;
  }

  private static ApiClient newApiClient(String targetServiceAccount, String fcBaseUrl)
      throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(fcBaseUrl);
    apiClient.setAccessToken(getAccessToken(targetServiceAccount));

    return apiClient;
  }
}
