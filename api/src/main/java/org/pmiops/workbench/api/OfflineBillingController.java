package org.pmiops.workbench.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingBatchUpdateService freeTierBillingService;
  private final GoogleProjectPerCostDao googleProjectPerCostDao;
  private TaskQueueService taskQueueService;

  private final UserService userService;

  @Autowired
  OfflineBillingController(
      FreeTierBillingBatchUpdateService freeTierBillingService,
      GoogleProjectPerCostDao googleProjectPerCostDao,
      UserService userService,
      TaskQueueService taskQueueService) {
    this.freeTierBillingService = freeTierBillingService;
    this.taskQueueService = taskQueueService;
    this.userService = userService;
    this.googleProjectPerCostDao = googleProjectPerCostDao;
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

    List<DbGoogleProjectPerCost> googleProjectCostList =
        freeTierForAllWorkspace.entrySet().stream()
            .map(DbGoogleProjectPerCost::new)
            .collect(Collectors.toList());

    // Clear table googleproject_cost and then insert all entries from BQ
    googleProjectPerCostDao.deleteAll();
    googleProjectPerCostDao.batchInsertProjectPerCost(googleProjectCostList);

    System.out.println("Table updated, create task for queue now");
    List<Long> allUserIds = userService.getAllUserIds();

    taskQueueService.groupAndPushFreeTierBilling(allUserIds);
    return ResponseEntity.noContent().build();
  }
}
