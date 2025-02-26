package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.util.Optional;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CreateVwbPodTaskRequest;
import org.pmiops.workbench.user.VwbUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskVwbController implements CloudTaskVwbApiDelegate {

  private final VwbUserService vwbUserService;
  private final UserService userService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public CloudTaskVwbController(
      VwbUserService vwbUserService,
      UserService userService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.vwbUserService = vwbUserService;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<Void> processCreateVwbPodTask(CreateVwbPodTaskRequest body) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBPodCreation) {
      return null;
    }
    Optional<DbUser> dbUser = userService.getByUsername(body.getUserName());
    // If the user exists and the user does not have a pod, create a pod for the user
    if (dbUser.isPresent() && dbUser.get().getVwbUserPod() == null) {
      vwbUserService.createInitialCreditsPodForUser(dbUser.get());
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.noContent().build();
  }
}
