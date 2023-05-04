package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.MethodRepositoryApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;

public abstract class ToolsFirecloudApiClientFactory {

  protected ApiClient apiClient;

  protected ToolsFirecloudApiClientFactory(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  protected static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  public ProfileApi profileApi() throws IOException {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }

  public MethodRepositoryApi methodRepositoryApi() throws IOException {
    MethodRepositoryApi api = new MethodRepositoryApi();
    api.setApiClient(apiClient);
    return api;
  }
}
