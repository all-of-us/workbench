package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.cloud.bigquery.TableResult;
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class BillingAlertsServiceTest {

  @Autowired
  BigQueryService bigQueryService;

  @Autowired
  BillingAlertsService billingAlertsService;

  @Autowired
  UserDao userDao;

  @Autowired
  NotificationService notificationService;

  @Autowired
  WorkspaceDao workspaceDao;

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({
      BillingAlertsService.class
  })
  @MockBean({
      BigQueryService.class,
      NotificationService.class
  })
  static class Configuration {
    @Bean
    @Scope("prototype")
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() throws Exception {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();

    InputStream inputStream = getClass()
        .getClassLoader().getResourceAsStream("bigquery/get_billing_project_costs.ser");
    TableResult tableResult = (TableResult) (new ObjectInputStream(inputStream)).readObject();

    doReturn(tableResult).when(bigQueryService).executeQuery(any());
  }

  @Test
  public void findCreatorByWorkspaceNamespace() {
    User user = createUser("test@test.com");
    createWorkspace(user, "rumney");
    createWorkspace(user, "rumney");

    assertThat(userDao.findCreatorByWorkspaceNamespace("rumney")).isEqualTo(user);
  }

  @Test
  public void alertUsersExceedingFreeTierBilling_singleProjectExceedsLimit() {
    workbenchConfig.freeCredits.limit = 100.0;

    User user = createUser("test@test.com");
    createWorkspace(user, "aou-test-f1-26");

    billingAlertsService.alertUsersExceedingFreeTierBilling();
    verify(notificationService).alertUser(eq(user), any());
  }

  @Test
  public void alertUsersExceedingFreeTierBilling_noAlert() {
    workbenchConfig.freeCredits.limit = 500.0;

    User user = createUser("test@test.com");
    createWorkspace(user, "aou-test-f1-26");

    billingAlertsService.alertUsersExceedingFreeTierBilling();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void alertUsersExceedingFreeTierBilling_workspaceMissingCreator() {
    workbenchConfig.freeCredits.limit = 500.0;

    User user = createUser("test@test.com");
    createWorkspace(user, "aou-test-f1-26");
    createWorkspace(null, "rumney");

    billingAlertsService.alertUsersExceedingFreeTierBilling();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void alertUsersExceedingFreeTierBilling_combinedProjectsExceedsLimit() {
    workbenchConfig.freeCredits.limit = 500.0;

    User user = createUser("test@test.com");
    createWorkspace(user, "aou-test-f1-26");
    createWorkspace(user, "aou-test-f1-47");

    billingAlertsService.alertUsersExceedingFreeTierBilling();
    verify(notificationService).alertUser(eq(user), any());
  }

  @Test
  public void alertUsersExceedingFreeTierBilling_whitelist() {
    workbenchConfig.freeCredits.limit = 500.0;

    User user = createUser("test@test.com");
    createWorkspace(user, "aou-test-f1-26");
    createWorkspace(user, "aou-test-f1-47");

    workbenchConfig.freeCredits.whitelistedBillingProjects.add("aou-test-f1-47");

    billingAlertsService.alertUsersExceedingFreeTierBilling();
    verifyZeroInteractions(notificationService);
  }

  // TODO: active? user and workspace?

  private User createUser(String email) {
    User user = new User();
    user.setEmail(email);
    return userDao.save(user);
  }

  private void createWorkspace(User creator, String namespace) {
    Workspace workspace = new Workspace();
    workspace.setCreator(creator);
    workspace.setWorkspaceNamespace(namespace);
    workspaceDao.save(workspace);
  }
}
