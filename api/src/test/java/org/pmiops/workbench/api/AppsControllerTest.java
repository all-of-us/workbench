package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
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
  @MockBean({
    InteractiveAnalysisService.class,
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
    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID);
    doReturn(testWorkspace).when(mockWorkspaceService).lookupWorkspaceByNamespace((WORKSPACE_NS));
  }

  @Test
  public void testCreateAppSuccess() {
    controller.createApp(WORKSPACE_NS, createAppRequest);
    verify(mockLeonardoApiClient).createApp(createAppRequest, testWorkspace);
  }

  @Test
  public void testCreateAppFail_validateInitialCreditUsage() {
    doThrow(new ForbiddenException())
        .when(mockWorkspaceAuthService)
        .validateInitialCreditUsage(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(
        ForbiddenException.class, () -> controller.createApp(WORKSPACE_NS, createAppRequest));
  }

  @Test
  public void testCreateCromwellAppSuccess() {
    CreateAppRequest createCromwellAppRequest = new CreateAppRequest().appType(AppType.CROMWELL);
    controller.createApp(WORKSPACE_NS, createCromwellAppRequest);
    verify(mockLeonardoApiClient).createApp(createCromwellAppRequest, testWorkspace);
  }

  @Test
  public void testCreateRStudioAppSuccess() {
    CreateAppRequest createRStudioAppRequest = new CreateAppRequest().appType(AppType.RSTUDIO);
    controller.createApp(WORKSPACE_NS, createRStudioAppRequest);
    verify(mockLeonardoApiClient).createApp(createRStudioAppRequest, testWorkspace);
  }

  @Test
  public void testCreateSASAppSuccess() {
    CreateAppRequest createSASAppRequest = new CreateAppRequest().appType(AppType.SAS);
    controller.createApp(WORKSPACE_NS, createSASAppRequest);
    verify(mockLeonardoApiClient).createApp(createSASAppRequest, testWorkspace);
  }

  @Test
  public void testListAppSuccess() {
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
  public void testCreateApp_securitySuspended() {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));
    assertThrows(
        FailedPreconditionException.class,
        () -> controller.createApp(WORKSPACE_NS, new CreateAppRequest().appType(AppType.RSTUDIO)));
  }
}
