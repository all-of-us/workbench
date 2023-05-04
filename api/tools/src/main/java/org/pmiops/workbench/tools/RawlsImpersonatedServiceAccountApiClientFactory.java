package org.pmiops.workbench.tools;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.pmiops.workbench.rawls.ApiClient;

public class RawlsImpersonatedServiceAccountApiClientFactory extends RawlsApiClientFactory {

  public RawlsImpersonatedServiceAccountApiClientFactory(
      String targetServiceAccount, String rawlsBaseUrl) throws IOException {
    super(newApiClient(targetServiceAccount, rawlsBaseUrl));
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

  private static ApiClient newApiClient(String targetServiceAccount, String rawlsBaseUrl)
      throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(rawlsBaseUrl);
    apiClient.setAccessToken(getAccessToken(targetServiceAccount));

    return apiClient;
  }
}
