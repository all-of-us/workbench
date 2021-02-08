package org.pmiops.workbench.tools;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Arrays;

import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.api.*;

import static org.pmiops.workbench.firecloud.FireCloudServiceImpl.FIRECLOUD_API_OAUTH_SCOPES;

public class ServiceAccountAPIClientFactory {

  final String apiUrl;

  private static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  public ServiceAccountAPIClientFactory(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public ApiClient newFirecloudAdminApiClient(String apiUrl) throws IOException {
    final OAuth2Credentials delegatedCreds =
            new DelegatedUserCredentials(
                    "all-of-us-workbench-test@appspot.gserviceaccount.com",
                    "firecloud-admin@all-of-us-workbench-test.iam.gserviceaccount.com",
                    FIRECLOUD_API_OAUTH_SCOPES,
                    IamCredentialsClient.create(),
                    new ApacheHttpTransport());
    delegatedCreds.refresh();

//    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(delegatedCreds.getAccessToken().getTokenValue());

    return apiClient;
  }


  private ApiClient newServiceAccountApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
//    GoogleCredentials credentials =
//        GoogleCredentials.getApplicationDefault().createScoped(Arrays.asList(FC_SCOPES));
//    credentials.refresh();

    apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(FC_SCOPES)));
    return apiClient;
  }

  private ApiClient newApiClient(String apiUrl) throws IOException {
    return newFirecloudAdminApiClient(apiUrl);
  }

  public WorkspacesApi workspacesApi() throws IOException {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  public BillingApi billingApi() throws IOException {
    BillingApi api = new BillingApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  public SubmissionsApi submissionsApi() throws IOException {
    SubmissionsApi api = new SubmissionsApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  public MethodconfigsApi methodconfigsApi() throws IOException {
    MethodconfigsApi api = new MethodconfigsApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }

  public ProfileApi profileApi() throws IOException {
    ProfileApi api = new ProfileApi();
    api.setApiClient(newApiClient(apiUrl));
    return api;
  }
}
