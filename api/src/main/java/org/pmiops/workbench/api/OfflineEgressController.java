package org.pmiops.workbench.api;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.EgressEventStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineEgressController implements OfflineEgressApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineEgressController.class.getName());
  private static final Duration PENDING_EVENT_LIMIT = Duration.ofHours(1L);

  @Autowired private Clock clock;
  @Autowired private TaskQueueService taskQueueService;
  @Autowired private EgressEventDao egressEventDao;

  @Override
  public ResponseEntity<Void> checkPendingEgressEvents() {
    Timestamp latestModifiedTime = Timestamp.from(clock.instant().minus(PENDING_EVENT_LIMIT));

    List<Long> oldPendingEventIds =
        egressEventDao
            .findAllByStatusAndLastModifiedTimeLessThan(
                EgressEventStatus.PENDING, latestModifiedTime)
            .stream()
            .map(DbEgressEvent::getEgressEventId)
            .collect(Collectors.toList());
    for (long eventId : oldPendingEventIds) {
      taskQueueService.pushEgressEventTask(eventId);
    }

    log.warning(
        String.format("found and re-enqueued %d old PENDING events", oldPendingEventIds.size()));
    return ResponseEntity.noContent().build();
  }
}
