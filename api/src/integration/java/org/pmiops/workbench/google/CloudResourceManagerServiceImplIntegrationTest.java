package org.pmiops.workbench.google;

import com.google.api.services.cloudresourcemanager.model.Project;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.IntegrationTestConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringRunner.class)
public class CloudResourceManagerServiceImplIntegrationTest {
  @Autowired private CloudResourceManagerService service;

  @TestConfiguration
  @Import({CloudResourceManagerServiceImpl.class, IntegrationTestConfig.class})
  static class Configuration {}

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
