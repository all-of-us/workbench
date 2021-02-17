package org.pmiops.workbench.tools;

import java.io.IOException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;

public abstract class ApiClientFactory {

  protected ApiClient apiClient;

  protected ApiClientFactory(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  protected static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  public WorkspacesApi workspacesApi() throws IOException {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  public BillingApi billingApi() throws IOException {
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }

  public ProfileApi profileApi() throws IOException {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }
}
