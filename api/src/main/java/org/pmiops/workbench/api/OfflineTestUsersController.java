package org.pmiops.workbench.api;

import static org.pmiops.workbench.firecloud.IntegrationTestUsers.COMPLIANT_USER;

import jakarta.inject.Provider;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.impersonation.ImpersonatedUserService;
import org.pmiops.workbench.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  private final ImpersonatedUserService impersonatedUserService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public OfflineTestUsersController(
      ImpersonatedUserService impersonatedUserService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.impersonatedUserService = impersonatedUserService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
    LOGGER.info("deleteAllTestUserWorkspaces is decommissioned...");
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspacesOrphanedInRawls() {
    LOGGER.info("deleteAllTestUserWorkspacesOrphanedInRawls is decommissioned...");
    return ResponseEntity.ok().build();
  }
}
