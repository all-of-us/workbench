package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import org.pmiops.workbench.firecloud.ApiClient;

public class ToolsFirecloudImpersonatedServiceAccountApiClientFactory
    extends ToolsFirecloudApiClientFactory {

  public ToolsFirecloudImpersonatedServiceAccountApiClientFactory(
      String targetServiceAccount, String fcBaseUrl) throws IOException {
    super(newApiClient(targetServiceAccount, fcBaseUrl));
  }

  private static ApiClient newApiClient(String targetServiceAccount, String fcBaseUrl)
      throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(fcBaseUrl);
    apiClient.setAccessToken(getAccessToken(targetServiceAccount));

    return apiClient;
  }
}
