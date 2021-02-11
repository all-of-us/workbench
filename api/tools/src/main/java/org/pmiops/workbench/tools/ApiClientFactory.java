package org.pmiops.workbench.tools;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.*;

public abstract class ApiClientFactory {

    protected ApiClient apiClient;
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

    public SubmissionsApi submissionsApi() throws IOException {
        SubmissionsApi api = new SubmissionsApi();
        api.setApiClient(apiClient);
        return api;
    }

    public MethodconfigsApi methodconfigsApi() throws IOException {
        MethodconfigsApi api = new MethodconfigsApi();
        api.setApiClient(apiClient);
        return api;
    }

    public ProfileApi profileApi() throws IOException {
        ProfileApi api = new ProfileApi();
        api.setApiClient(apiClient);
        return api;
    }
}
