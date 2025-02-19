package org.pmiops.workbench.api;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.pmiops.workbench.initialcredits.InitialCreditsBatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final InitialCreditsBatchUpdateService freeTierBillingService;
  private final GoogleProjectPerCostDao googleProjectPerCostDao;
  private final TaskQueueService taskQueueService;

  private final UserService userService;

  @Autowired
  OfflineBillingController(
      InitialCreditsBatchUpdateService freeTierBillingService,
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
    // Get cost for all workspace from BQ
    Map<String, Double> freeTierForAllWorkspace =
        freeTierBillingService.getFreeTierWorkspaceCostsFromBQ();

    List<DbGoogleProjectPerCost> googleProjectCostList =
        freeTierForAllWorkspace.entrySet().stream().map(DbGoogleProjectPerCost::new).toList();

    // Clear table googleproject_cost and then insert all entries from BQ
    googleProjectPerCostDao.deleteAll();
    googleProjectPerCostDao.batchInsertProjectPerCost(googleProjectCostList);
    log.info("Inserted all workspace costs to googleproject_cost table");

    List<Long> allUserIds = userService.getAllUserIds();

    taskQueueService.groupAndPushFreeTierBilling(allUserIds);
    log.info("Pushed all users to Cloud Task for Free Tier Billing");

    return ResponseEntity.noContent().build();
  }
}
