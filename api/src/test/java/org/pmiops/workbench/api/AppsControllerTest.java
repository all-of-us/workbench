package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AppsControllerTest {
  @TestConfiguration
  @Import({
    AppsController.class,
    FakeClockConfiguration.class,
    LeonardoApiHelper.class,
  })
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }
  }

  @Autowired private AppsController controller;

  @MockBean LeonardoApiClient mockLeonardoApiClient;
  @MockBean WorkspaceAuthService mockWorkspaceAuthService;
  @MockBean WorkspaceService mockWorkspaceService;

  private static final String APP_NAME = "all-of-us-123-cromwell";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String WORKSPACE_NAME = "workspace name";
  private static final String WORKSPACE_ID = "myfirstworkspace";

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbWorkspace testWorkspace;
  private static DbUser user;
  private CreateAppRequest createAppRequest;

  @BeforeEach
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();

    user = new DbUser();
    createAppRequest = new CreateAppRequest().appType(AppType.RSTUDIO);
    config.featureFlags.enableRStudioGKEApp = true;
    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID);
    doReturn(testWorkspace).when(mockWorkspaceService).lookupWorkspaceByNamespace((WORKSPACE_NS));
  }

  @Test
  public void testCreateAppSuccess() throws Exception {
    controller.createApp(WORKSPACE_NS, createAppRequest);
    verify(mockLeonardoApiClient).createApp(createAppRequest, testWorkspace);
  }

  @Test
  public void testCreateAppFail_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(
        ForbiddenException.class, () -> controller.createApp(WORKSPACE_NS, createAppRequest));
  }

  @Test
  public void testCreateCromwellAppFail_featureNotEnabled() throws Exception {
    config.featureFlags.enableCromwellGKEApp = false;
    CreateAppRequest createCromwellAppRequest = new CreateAppRequest().appType(AppType.CROMWELL);
    assertThrows(
        UnsupportedOperationException.class,
        () -> controller.createApp(WORKSPACE_NS, createCromwellAppRequest));
  }

  @Test
  public void testCreateRStudioAppFail_featureNotEnabled() throws Exception {
    config.featureFlags.enableRStudioGKEApp = false;
    CreateAppRequest createRStudioAppRequest = new CreateAppRequest().appType(AppType.RSTUDIO);
    assertThrows(
        UnsupportedOperationException.class,
        () -> controller.createApp(WORKSPACE_NS, createRStudioAppRequest));
  }

  @Test
  public void testGetAppSuccess() throws Exception {
    controller.getApp(WORKSPACE_NS, APP_NAME);
    verify(mockLeonardoApiClient).getAppByNameByProjectId(GOOGLE_PROJECT_ID, APP_NAME);
  }

  @Test
  public void testListAppSuccess() throws Exception {
    controller.listAppsInWorkspace(WORKSPACE_NS);
    verify(mockLeonardoApiClient).listAppsInProjectCreatedByCreator(GOOGLE_PROJECT_ID);
  }

  @Test
  public void testDeleteAppSuccess() {
    boolean deleteDisk = true;
    controller.deleteApp(WORKSPACE_NS, APP_NAME, deleteDisk);
    verify(mockLeonardoApiClient).deleteApp(APP_NAME, testWorkspace, deleteDisk);
  }

  @Test
  public void testCanPerformAppActions() {
    // does not throw
    controller.validateCanPerformApiAction(testWorkspace);
  }

  @Test
  public void testCanPerformAppActions_securitySuspended() throws ApiException {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));
    assertThrows(
        FailedPreconditionException.class,
        () -> controller.validateCanPerformApiAction(testWorkspace));
  }

  @Test
  public void testCanPerformAppActions_noWorkspacePermission() throws ApiException {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    assertThrows(
        ForbiddenException.class, () -> controller.validateCanPerformApiAction(testWorkspace));
  }
}
