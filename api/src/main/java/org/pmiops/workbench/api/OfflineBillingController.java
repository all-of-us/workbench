package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineBillingController.class.getName());

  private final TaskQueueService taskQueueService;
  private final UserService userService;

  @Autowired
  OfflineBillingController(TaskQueueService taskQueueService, UserService userService) {
    this.taskQueueService = taskQueueService;
    this.userService = userService;
  }

  @Override
  public ResponseEntity<Void> checkInitialCreditsUsage() {
    log.info("Checking initial credits usage for all workspaces");
    taskQueueService.groupAndPushInitialCreditsUsage(userService.getAllUserIds());
    log.info("Pushed all users to the Cloud Task endpoint checkInitialCreditsUsage");
    return ResponseEntity.noContent().build();
  }
}
