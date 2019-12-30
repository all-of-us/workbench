package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.ServiceAccount;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public class FireCloudIntegrationTest {

  /*
   * Mocked service providers are (currently) not available for functional integration tests.
   * Integration tests with real users/workspaces against production FireCloud is not
   * recommended at this time.
   */
  @Mock private BillingApi billingApi;
  @Mock private WorkspacesApi workspacesApi;
  @Mock private GroupsApi allOfUsGroupsApi;
  @Mock private WorkspacesApi workspaceAclsApi;
  @Mock private StaticNotebooksApi staticNotebooksApi;
  @Mock private ProfileApi profileApi;
  @Mock private NihApi nihApi;

  // N.B. this will load the default service account credentials for whatever AoU environment
  // is set when running integration tests. This should be the test environment.
  @Autowired
  @Qualifier(Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
  private ServiceAccountCredentials defaultServiceAccountCredentials;

  @Autowired private ServiceAccounts serviceAccounts;

  @Autowired
  @Qualifier(Constants.FIRECLOUD_ADMIN_CREDS)
  private ServiceAccountCredentials fireCloudAdminCredentials;

  @Before
  public void setUp() throws IOException {
  }

  /**
   * Creates a FireCloudService instance with the FireCloud base URL corresponding to the given
   * WorkbenchConfig. Note that this will always use the test environment's default service account
   * credentials when making API calls. It shouldn't be possible to make authenticated calls to the
   * FireCloud prod environment.
   *
   * <p>This method mostly exists to allow us to run a status-check against both FC dev & prod
   * within the same integration test run.
   */
  private FireCloudService createService(WorkbenchConfig config) throws IOException {
    ServiceAccountCredentials scopedCreds = (ServiceAccountCredentials) defaultServiceAccountCredentials.createScoped(FireCloudServiceImpl.FIRECLOUD_API_OAUTH_SCOPES);
    scopedCreds.refresh();

    ApiClient apiClient =
        new ApiClient()
            .setBasePath(config.firecloud.baseUrl)
            .setDebugging(config.firecloud.debugEndpoints);
    apiClient.setAccessToken(scopedCreds.getAccessToken().getTokenValue());

    return new FireCloudServiceImpl(
        Providers.of(config),
        Providers.of(profileApi),
        Providers.of(billingApi),
        Providers.of(allOfUsGroupsApi),
        Providers.of(nihApi),
        Providers.of(workspacesApi),
        Providers.of(workspaceAclsApi),
        Providers.of(new StatusApi(apiClient)),
        Providers.of(staticNotebooksApi),
        new FirecloudRetryHandler(new NoBackOffPolicy()),
        serviceAccounts,
        Providers.of(fireCloudAdminCredentials));
  }

  private WorkbenchConfig loadConfig(String filename) throws Exception {
    String testConfig =
        Resources.toString(Resources.getResource(filename), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }

  private FireCloudService getTestService() throws Exception {
    return createService(loadConfig("config_test.json"));
  }

  private FireCloudService getProdService() throws Exception {
    return createService(loadConfig("config_prod.json"));
  }

  @Test
  public void testStatusProd() throws Exception {
    assertThat(getProdService().getFirecloudStatus()).isTrue();
  }

  @Test
  public void testStatusDev() throws Exception {
    assertThat(getTestService().getFirecloudStatus()).isTrue();
  }

  /**
   * Ensures we can successfully use delegation of authority to make FireCloud API calls on behalf
   * of AoU users.
   *
   * <p>This test depends on there being an active account in FireCloud dev with the email address
   * integration-test-user@fake-research-aou.org.
   */
  @Test
  public void testImpersonatedProfileCall() throws Exception {
    ApiClient apiClient =
        getTestService()
            .getApiClientWithImpersonation("integration-test-user@fake-research-aou.org");

    // Run the most basic API call against the /me/ endpoint.
    ProfileApi profileApi = new ProfileApi(apiClient);
    FirecloudMe me = profileApi.me();
    assertThat(me.getUserInfo().getUserEmail())
        .isEqualTo("integration-test-user@fake-research-aou.org");
    assertThat(me.getUserInfo().getUserSubjectId()).isEqualTo("101727030557929965916");

    // Run a test against a different FireCloud endpoint. This is important, because the /me/
    // endpoint is accessible even by service accounts whose subject IDs haven't been whitelisted
    // by FireCloud devops.
    //
    // If we haven't had our "firecloud-admin" service account whitelisted,
    // then the following API call would result in a 401 error instead of a 404.
    NihApi nihApi = new NihApi(apiClient);
    int responseCode = 0;
    try {
      nihApi.nihStatus();
    } catch (ApiException e) {
      responseCode = e.getCode();
    }
    assertThat(responseCode).isEqualTo(404);
  }
}
