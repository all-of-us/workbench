package org.pmiops.workbench.exfiltration;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressEventServiceImpl implements EgressEventService {

  private static final Logger logger = Logger.getLogger(EgressEventServiceImpl.class.getName());
  private final EgressEventAuditor egressEventAuditor;
  private final UserService userService;
  private final TaskQueueService taskQueueService;
  private final EgressEventDao egressEventDao;

  @Autowired
  public EgressEventServiceImpl(
      EgressEventAuditor egressEventAuditor,
      UserService userService,
      TaskQueueService taskQueueService,
      EgressEventDao egressEventDao) {
    this.egressEventAuditor = egressEventAuditor;
    this.userService = userService;
    this.taskQueueService = taskQueueService;
    this.egressEventDao = egressEventDao;
  }

  @Override
  public void handleVwbEvent(VwbEgressEventRequest egressEvent) {
    Optional<DbUser> egressUser = userService.getByUsername(egressEvent.getUserEmail());
    if (egressUser.isEmpty()) {
      logger.severe(
          String.format(
              "An egress event happened for workspace: %s, but we're unable to find a user %s.",
              egressEvent.getVwbWorkspaceId(), egressEvent.getUserEmail()));
    }
    egressEventAuditor.fireVwbEgressEvent(egressEvent, egressUser.get());
    DbEgressEvent event =
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(egressUser.orElse(null))
                .setVwbEgressEventId(egressEvent.getVwbEgressEventId())
                .setVwbWorkspaceId(egressEvent.getVwbWorkspaceId())
                .setVwbVmName(egressEvent.getVmName())
                .setTimeWindowStart(new Timestamp(egressEvent.getTimeWindowStart()))
                .setGcpProjectId(egressEvent.getGcpProjectId())
                .setVwbIncidentCount(egressEvent.getIncidentCount().intValue())
                .setEgressMegabytes(
                    Optional.ofNullable(egressEvent.getEgressMib())
                        // Mebibytes (2^20 bytes) -> Megabytes (10^6 bytes)
                        .map(mib -> (float) (mib * ((1 << 20) / 1e6)))
                        .orElse(null))
                .setEgressWindowSeconds(egressEvent.getTimeWindowDuration())
                .setStatus(DbEgressEventStatus.PENDING));
    taskQueueService.pushEgressEventTask(event.getEgressEventId(), true);
  }
}
