package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
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
  private static final Logger log = Logger.getLogger(OfflineBillingController.class.getName());

  private final GoogleProjectPerCostDao googleProjectPerCostDao;
  private final InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final TaskQueueService taskQueueService;
  private final UserService userService;

  @Autowired
  OfflineBillingController(
      GoogleProjectPerCostDao googleProjectPerCostDao,
      InitialCreditsBatchUpdateService initialCreditsBatchUpdateService,
      Provider<Stopwatch> stopwatchProvider,
      TaskQueueService taskQueueService,
      UserService userService) {
    this.initialCreditsBatchUpdateService = initialCreditsBatchUpdateService;
    this.googleProjectPerCostDao = googleProjectPerCostDao;
    this.stopwatchProvider = stopwatchProvider;
    this.taskQueueService = taskQueueService;
    this.userService = userService;
  }

  @Override
  public ResponseEntity<Void> checkInitialCreditsUsage() {
    log.info("Checking initial credits usage for all workspaces");

    // Get cost for all workspace from BQ
    // temp performance logging
    Stopwatch stopwatch = stopwatchProvider.get().start();
    List<DbGoogleProjectPerCost> workspaceCosts =
        initialCreditsBatchUpdateService.getWorkspaceCostsFromBQ().entrySet().stream()
            .map(DbGoogleProjectPerCost::new)
            .toList();
    Duration elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: Retrieved %d workspace cost entries from BigQuery in %s",
            workspaceCosts.size(), formatDurationPretty(elapsed)));

    stopwatch.reset().start();
    // Clear table googleproject_cost and then insert all entries from BQ
    googleProjectPerCostDao.deleteAll();
    elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: cleared googleproject_cost table in %s",
            formatDurationPretty(elapsed)));

    stopwatch.reset().start();
    googleProjectPerCostDao.batchInsertProjectPerCost(workspaceCosts);
    elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: Inserted all workspace costs to googleproject_cost table in %s",
            formatDurationPretty(elapsed)));

    taskQueueService.groupAndPushInitialCreditsUsage(userService.getAllUserIds());
    log.info("Pushed all users to the Cloud Task endpoint checkInitialCreditsUsage");

    return ResponseEntity.noContent().build();
  }
}
