package org.pmiops.workbench.api;

import static org.pmiops.workbench.firecloud.IntegrationTestUsers.COMPLIANT_USER;

import jakarta.inject.Provider;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.impersonation.ImpersonatedUserService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.model.TestUserRawlsWorkspace;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.utils.UserUtils;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  private final ImpersonatedUserService impersonatedUserService;
  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final TaskQueueService taskQueueService;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public OfflineTestUsersController(
      ImpersonatedUserService impersonatedUserService,
      ImpersonatedWorkspaceService impersonatedWorkspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService,
      WorkspaceMapper workspaceMapper) {
    this.impersonatedUserService = impersonatedUserService;
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
    this.taskQueueService = taskQueueService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceMapper = workspaceMapper;
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
      LOGGER.info("Ensuring test user TOS compliance...");
      testUserConf.testUserEmails.forEach(this::ensureTosCompliance);
      LOGGER.info("Done ensuring test user TOS compliance.");
    }

    return ResponseEntity.ok().build();
  }

  private void ensureTosCompliance(String username) {
    var currentStatus = impersonatedUserService.getTerraTermsOfServiceStatusForUser(username);
    if (currentStatus.getIsCurrentVersion()) {
      LOGGER.info(
          String.format(
              "Test user %s is already compliant with the latest Terra Terms of Service",
              username));
    } else {
      LOGGER.info(String.format("Accepting the Terra Terms of Service for test user %s", username));
      impersonatedUserService.acceptTerraTermsOfServiceForUser(username);
    }
  }

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspaces() {
    LOGGER.info("Deleting test user workspaces...");

    deleteWorkspaces(
        this::enumerateAoUWorkspaces, taskQueueService::groupAndPushDeleteTestWorkspaceTasks);

    LOGGER.info("Done deleting test user workspaces.");
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspacesOrphanedInRawls() {
    LOGGER.info("Deleting test user workspaces in Rawls (Terra)...");

    deleteWorkspaces(
        this::enumerateRawlsWorkspaces,
        taskQueueService::groupAndPushDeleteTestWorkspaceInRawlsTasks);

    LOGGER.info("Done deleting test user workspaces in Rawls (Terra)...");
    return ResponseEntity.ok().build();
  }

  private <T> void deleteWorkspaces(
      Function<String, Stream<T>> enumerator, Consumer<List<T>> queue) {
    WorkbenchConfig config = workbenchConfigProvider.get();
    WorkbenchConfig.E2ETestUserConfig testUserConf = config.e2eTestUsers;

    // only some environments have test users
    if (testUserConf == null) {
      LOGGER.info("This environment does not have a test user config block.  Exiting.");
    } else {
      queue.accept(testUserConf.testUserEmails.stream().flatMap(enumerator).toList());
    }
  }

  private Stream<TestUserWorkspace> enumerateAoUWorkspaces(String username) {
    List<WorkspaceResponse> workspaces = impersonatedWorkspaceService.getOwnedWorkspaces(username);
    String action = workspaces.isEmpty() ? "." : "; queueing for deletion";
    LOGGER.info(
        String.format(
            "Test user %s currently owns %d workspaces%s", username, workspaces.size(), action));

    return workspaces.stream()
        .map(ws -> workspaceMapper.toTestUserWorkspace(ws.getWorkspace(), username));
  }

  private Stream<TestUserRawlsWorkspace> enumerateRawlsWorkspaces(String username) {
    List<RawlsWorkspaceListResponse> workspaces =
        impersonatedWorkspaceService.getOwnedWorkspacesOrphanedInRawls(username);
    String action = workspaces.isEmpty() ? "." : "; queueing for deletion";
    LOGGER.info(
        String.format(
            "Test user %s currently owns %d workspaces in Rawls%s",
            username, workspaces.size(), action));

    return workspaces.stream()
        .map(ws -> workspaceMapper.toTestUserRawlsWorkspace(ws.getWorkspace(), username));
  }
}
