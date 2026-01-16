package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.util.Optional;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.CreateVwbPodTaskRequest;
import org.pmiops.workbench.user.VwbUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskVwbController implements CloudTaskVwbApiDelegate {

  private static final Logger log = LoggerFactory.getLogger(CloudTaskVwbController.class.getName());

  private final VwbUserService vwbUserService;
  private final UserService userService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final InitialCreditsService initialCreditsService;

  public CloudTaskVwbController(
      VwbUserService vwbUserService,
      UserService userService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      InitialCreditsService initialCreditsService) {
    this.vwbUserService = vwbUserService;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.initialCreditsService = initialCreditsService;
  }

  @Override
  public ResponseEntity<Void> processCreateVwbPodTask(CreateVwbPodTaskRequest body) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBPodCreation) {
      return ResponseEntity.badRequest().build();
    }
    Optional<DbUser> dbUser = userService.getByUsername(body.getUserName());
    // If the user exists, create or complete the pod creation
    if (dbUser.isPresent()) {
      DbUser user = dbUser.get();
      // Always call createInitialCreditsPodForUser - it will check internally if pod already exists
      vwbUserService.createInitialCreditsPodForUser(user);

      // If the user has run out of initial credits, unlink the billing account to prevent further
      // spending
      if (initialCreditsService.hasUserRunOutOfInitialCredits(user)
          || initialCreditsService.areUserCreditsExpired(user)) {
        log.debug(
            "User has run out of initial credits, unlinking billing account for user: "
                + user.getUsername());
        vwbUserService.unlinkBillingAccountForUserPod(user);
      }
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.noContent().build();
  }
}
