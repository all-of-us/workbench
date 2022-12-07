package org.pmiops.workbench.api;

import static org.pmiops.workbench.firecloud.IntegrationTestUsers.COMPLIANT_USER;

import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;

  @Autowired
  public OfflineTestUsersController(
      Provider<WorkbenchConfig> workbenchConfigProvider, UserService userService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
  }

  public ResponseEntity<Void> ensureTestUserTosCompliance() {
    WorkbenchConfig config = workbenchConfigProvider.get();

    // TODO: is there a better way to check for when we're executing in the test env?
    if (config.server.projectId.equals("all-of-us-workbench-test")) {
      ensureTosStatus(COMPLIANT_USER);
    }

    WorkbenchConfig.E2ETestUserConfig testUserConf = config.e2eTestUsers;

    // only some environments have test users
    if (testUserConf == null) {
      LOGGER.info("This environment does not have a test user config block.  Exiting.");
    } else {
      testUserConf.testUserEmails.forEach(this::ensureTosStatus);
    }

    return ResponseEntity.ok().build();
  }

  private void ensureTosStatus(String username) {
    boolean desiredState = true; // TODO for reject
    boolean currentState = userService.getUserTerraTermsOfServiceStatusWithImpersonation(username);
    if (currentState == desiredState) {
      LOGGER.info(
          String.format(
              "Test user %s is already in desired TOS acceptance state: %s",
              username, desiredState));
    } else {
      LOGGER.info(
          String.format(
              "Updating test user %s to desired TOS acceptance state: %s", username, desiredState));
      if (desiredState) {
        userService.acceptTerraTermsOfServiceWithImpersonation(username);
      } // TODO else
    }
  }
}
