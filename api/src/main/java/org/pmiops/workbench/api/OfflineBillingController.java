package org.pmiops.workbench.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.model.UserBQCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private TaskQueueService taskQueueService;

  private final UserService userService;

  private final FreeTierBillingBatchUpdateService freeTierBillingService;
  private final WorkspaceDao workspaceDao;

  @Autowired
  OfflineBillingController(
      FreeTierBillingBatchUpdateService freeTierBillingService,
      TaskQueueService taskQueueService,
      UserService userService,
      WorkspaceDao workspaceDao) {
    this.freeTierBillingService = freeTierBillingService;
    this.taskQueueService = taskQueueService;
    this.userService = userService;
    this.workspaceDao = workspaceDao;
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    freeTierBillingService.checkFreeTierBillingUsage();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsageCloudTask() {
    // Get cost for all workspace from BQ
    Map<String, Double> freeTierForAllWorkspace =
        freeTierBillingService.getFreeTierWorkspaceCostsFromBQ();

    // Get all user IDS and then set BQ cost for all workspace user has
    List<Long> allUserIds = userService.getAllUserIds();

    List<UserBQCost> userBQCostList =
        allUserIds.stream()
            .map(userId -> getAllWorkspaceCostPerUser(userId, freeTierForAllWorkspace))
            .collect(Collectors.toList());

    taskQueueService.groupAndPushFreeTierBilling(userBQCostList);
    return ResponseEntity.noContent().build();
  }

  // Get all google project ids associated with user
  // Filter out the map entries from freeTierForAllWorkspace for all the google project ids/user
  private UserBQCost getAllWorkspaceCostPerUser(
      long userId, Map<String, Double> freeTierForAllWorkspace) {
    Set<String> googleProjectForUser = workspaceDao.getGoogleProjectForUser(userId);

    Map<String, Double> bqCostForAllUserWorkspaces =
        freeTierForAllWorkspace.entrySet().stream()
            .filter((entry) -> googleProjectForUser.contains(entry.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    return new UserBQCost().userId(userId).workspaceBQCost(bqCostForAllUserWorkspaces);
  }
}
