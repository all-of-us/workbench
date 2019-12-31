package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.services.cloudresourcemanager.model.Project;
import java.io.IOException;
import java.util.List;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.ServiceAccount;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.IntegrationTestConfig;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public class CloudResourceManagerServiceImplIntegrationTest {
  @Autowired
  private CloudResourceManagerService service;

  // This is a single hand created user in the fake-research-aou.org gsuite.
  // It has one project that has been shared with it, AoU CRM Integration Test
  // in the firecloud dev domain.
  private final String CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL =
      "cloud-resource-manager-integration-test@fake-research-aou.org";
  
  @Test
  public void testGetAllProjectsForUser() {
    DbUser testUser = new DbUser();
    testUser.setUsername(CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL);
    List<Project> projectList = service.getAllProjectsForUser(testUser);
    assertThat(projectList.size()).isEqualTo(1);
  }
}
