package org.pmiops.workbench.api;

import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private TaskQueueService taskQueueService;

  private final UserService userService;

  @Autowired
  OfflineBillingController(TaskQueueService taskQueueService, UserService userService) {
    this.taskQueueService = taskQueueService;
    this.userService = userService;
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    taskQueueService.groupAndPushFreeTierBilling(userService.getAllUserIds());
    return ResponseEntity.noContent().build();
  }
}
