package org.pmiops.workbench.api;

import static org.pmiops.workbench.firecloud.IntegrationTestUsers.COMPLIANT_USER;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.impersonation.ImpersonatedUserService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ImpersonatedUserService impersonatedUserService;
  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;
  private final TaskQueueService taskQueueService;

  @Autowired
  public OfflineTestUsersController(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ImpersonatedUserService impersonatedUserService,
      ImpersonatedWorkspaceService impersonatedWorkspaceService,
      TaskQueueService taskQueueService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.impersonatedUserService = impersonatedUserService;
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
    this.taskQueueService = taskQueueService;
  }

  @Override
  public ResponseEntity<Void> ensureTestUserTosCompliance() {
    WorkbenchConfig config = workbenchConfigProvider.get();

    // only run this on the Test env
    if (UserUtils.isUserInDomain(COMPLIANT_USER, config)) {
      ensureTosCompliance(COMPLIANT_USER);
    }

    WorkbenchConfig.E2ETestUserConfig testUserConf = config.e2eTestUsers;

    // only some environments have test users
    if (testUserConf == null) {
      LOGGER.info("This environment does not have a test user config block.  Exiting.");
    } else {
      testUserConf.testUserEmails.forEach(this::ensureTosCompliance);
    }

    return ResponseEntity.ok().build();
  }

  private void ensureTosCompliance(String username) {
    boolean currentState = impersonatedUserService.getUserTerraTermsOfServiceStatus(username);
    if (currentState) {
      LOGGER.info(
          String.format(
              "Test user %s is already compliant with the Terra Terms of Service", username));
    } else {
      LOGGER.info(String.format("Accepting the Terra Terms of Service for test user %s", username));
      impersonatedUserService.acceptTerraTermsOfService(username);
    }
  }

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspaces() {
    WorkbenchConfig config = workbenchConfigProvider.get();
    WorkbenchConfig.E2ETestUserConfig testUserConf = config.e2eTestUsers;

    // only some environments have test users
    if (testUserConf == null) {
      LOGGER.info("This environment does not have a test user config block.  Exiting.");
    } else {
      taskQueueService.groupAndPushDeleteTestWorkspaceTasks(
          testUserConf.testUserEmails.stream()
              .flatMap(this::enumerateWorkspaces)
              .collect(Collectors.toList()));
    }

    return ResponseEntity.ok().build();
  }

  private Stream<TestUserWorkspace> enumerateWorkspaces(String username) {
    List<WorkspaceResponse> workspaces = impersonatedWorkspaceService.getOwnedWorkspaces(username);
    LOGGER.info(
        String.format(
            "Test user %s currently owns %d workspaces; queueing for deletion",
            username, workspaces.size()));

    return workspaces.stream()
        .map(
            ws ->
                new TestUserWorkspace()
                    .username(username)
                    .wsNamespace(ws.getWorkspace().getNamespace())
                    .wsFirecloudId(ws.getWorkspace().getId()));
  }
}
