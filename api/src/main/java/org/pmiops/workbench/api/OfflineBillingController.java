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

  private final GoogleProjectPerCostDao googleProjectPerCostDao;
  private final InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;
  private final TaskQueueService taskQueueService;
  private final UserService userService;

  @Autowired
  OfflineBillingController(
      GoogleProjectPerCostDao googleProjectPerCostDao,
      InitialCreditsBatchUpdateService initialCreditsBatchUpdateService,
      TaskQueueService taskQueueService,
      UserService userService) {
    this.googleProjectPerCostDao = googleProjectPerCostDao;
    this.initialCreditsBatchUpdateService = initialCreditsBatchUpdateService;
    this.taskQueueService = taskQueueService;
    this.userService = userService;
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    // Get cost for all workspace from BQ
    Map<String, Double> workspaceCostsFromBQ =
        initialCreditsBatchUpdateService.getWorkspaceCostsFromBQ();

    List<DbGoogleProjectPerCost> googleProjectCostList =
        workspaceCostsFromBQ.entrySet().stream().map(DbGoogleProjectPerCost::new).toList();

    // Clear table googleproject_cost and then insert all entries from BQ
    googleProjectPerCostDao.deleteAll();
    googleProjectPerCostDao.batchInsertProjectPerCost(googleProjectCostList);
    log.info("Inserted all workspace costs to googleproject_cost table");

    List<Long> allUserIds = userService.getAllUserIds();

    taskQueueService.groupAndPushInitialCreditsUsage(allUserIds);
    log.info("Pushed all users to the Initial Credits Usage Cloud Task");

    return ResponseEntity.noContent().build();
  }
}
