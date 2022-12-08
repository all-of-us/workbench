package org.pmiops.workbench.api;

import static org.pmiops.workbench.firecloud.IntegrationTestUsers.COMPLIANT_USER;

import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.impersonation.ImpersonatedUserService;
import org.pmiops.workbench.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ImpersonatedUserService impersonatedUserService;

  @Autowired
  public OfflineTestUsersController(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ImpersonatedUserService impersonatedUserService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.impersonatedUserService = impersonatedUserService;
  }

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
}
