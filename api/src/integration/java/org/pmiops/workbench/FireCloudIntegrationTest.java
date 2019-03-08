package org.pmiops.workbench;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.NoBackOffPolicy;

import java.nio.charset.Charset;

import static com.google.common.truth.Truth.assertThat;

public class FireCloudIntegrationTest {

  /*
   * Mocked service providers are (currently) not available for functional integration tests.
   * Integration tests with real users/workspaces against production FireCloud is not
   * recommended at this time.
   */
  @Mock
  private ProfileApi profileApi;
  @Mock
  private BillingApi billingApi;
  @Mock
  private WorkspacesApi workspacesApi;
  @Mock
  private GroupsApi groupsApi;
  @Mock
  private NihApi nihApi;

  @Autowired
  @Qualifier(ServiceAccounts.FIRECLOUD_ADMIN_CREDS)
  private GoogleCredential fireCloudAdminCredential;

  private FireCloudServiceImpl fireCloudService;

  @Before
  public void setUp() throws Exception {
    WorkbenchConfig config = createConfig();
    fireCloudService = new FireCloudServiceImpl(
        Providers.of(config),
        Providers.of(profileApi),
        Providers.of(billingApi),
        // All of us groups api
        Providers.of(groupsApi),
        // End user groups api.
        Providers.of(groupsApi),
        Providers.of(nihApi),
        Providers.of(workspacesApi),
        Providers.of(new FireCloudConfig().statusApi(config)),
        new FirecloudRetryHandler(new NoBackOffPolicy()),
        Providers.of(fireCloudAdminCredential),
        new GoogleCredential.Builder(),
        new ApacheHttpTransport()
    );
  }

  @Test
  public void testStatus() {
    assertThat(fireCloudService.getFirecloudStatus()).isTrue();
  }

  private static WorkbenchConfig createConfig() throws Exception {
    String testConfig = Resources.toString(Resources.getResource("config_prod.json"), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }

}
