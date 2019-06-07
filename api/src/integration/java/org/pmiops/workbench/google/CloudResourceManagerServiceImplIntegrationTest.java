package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.services.cloudresourcemanager.model.Project;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.IntegrationTestConfig;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public class CloudResourceManagerServiceImplIntegrationTest {
  private CloudResourceManagerServiceImpl service;
  private final ApacheHttpTransport httpTransport = new ApacheHttpTransport();


  private final String CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL = "cloud-resource-manager-integration-test@fake-research-aou.org";


  // N.B. this will load the default service account credentials for whatever AoU environment
  // is set when running integration tests. This should be the test environment.
  @Autowired
  @Qualifier(Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
  private GoogleCredential serviceAccountCredential;

  @Autowired
  private ServiceAccounts serviceAccounts;

  @Autowired
  @Qualifier(Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
  private GoogleCredential cloudResourceManagerAdminCredential;

  @Before
  public void setUp() throws IOException {
    // Get a refreshed access token for the CloudResourceManager service account credentials.
    serviceAccountCredential = serviceAccountCredential.createScoped(
        CloudResourceManagerServiceImpl.SCOPES);
    serviceAccountCredential.refreshToken();
    service = new CloudResourceManagerServiceImpl(
        Providers.of(cloudResourceManagerAdminCredential),
        httpTransport, new GoogleRetryHandler(new NoBackOffPolicy()),
        serviceAccounts);;
  }

  @Test
  public void testGetAllProjectsForUser() {
    User testUser = new User();
    testUser.setEmail(CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL);
    List<Project> projectList = service.getAllProjectsForUser(testUser);
    assertThat(projectList.size()).isEqualTo(1);
  }
}
