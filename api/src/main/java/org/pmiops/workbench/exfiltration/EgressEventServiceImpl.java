package org.pmiops.workbench.exfiltration;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.gceVmNameToUserDatabaseId;
import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.gkeServiceNameToUserDatabaseId;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressEventServiceImpl implements EgressEventService {

  private static final Logger logger = Logger.getLogger(EgressEventServiceImpl.class.getName());
  private static final Duration MAXIMUM_EXPECTED_EVENT_AGE = Duration.ofMinutes(5);
  private final Clock clock;
  private final EgressEventAuditor egressEventAuditor;
  private final UserService userService;
  private final TaskQueueService taskQueueService;

  private final WorkspaceDao workspaceDao;
  private final EgressEventDao egressEventDao;

  // Workspace namespace placeholder in Egress alert when workspace namespace is missing.
  // It may happen when workspace is deleted from db, or we are not able to retrieve workspace
  // by google project id.
  @VisibleForTesting static final String NOT_FOUND_WORKSPACE_NAMESPACE = "NOT_FOUND";

  @Autowired
  public EgressEventServiceImpl(
      Clock clock,
      EgressEventAuditor egressEventAuditor,
      UserService userService,
      TaskQueueService taskQueueService,
      WorkspaceDao workspaceDao,
      EgressEventDao egressEventDao) {
    this.clock = clock;
    this.egressEventAuditor = egressEventAuditor;
    this.userService = userService;
    this.taskQueueService = taskQueueService;
    this.workspaceDao = workspaceDao;
    this.egressEventDao = egressEventDao;
  }

  @Override
  public void handleEvent(SumologicEgressEvent event) {
    // Lookup workspace by googleProject name, and set the workspaceNamespace in EgressEvent.
    Optional<DbWorkspace> dbWorkspaceMaybe =
        workspaceDao.getByGoogleProject(event.getProjectName());
    String workspaceNamespace;
    if (dbWorkspaceMaybe.isEmpty()) {
      logger.warning(
          String.format(
              "Workspace not found by given Google Project Id: %s", event.getProjectName()));
      workspaceNamespace = NOT_FOUND_WORKSPACE_NAMESPACE;
    } else {
      workspaceNamespace = dbWorkspaceMaybe.get().getWorkspaceNamespace();
    }

    Optional<DbUser> egressUser = findEgressUsers(event);

    // This would happen if we receive a second GKE Egress for the same event when the app is being
    // STOPPED,
    // we just audit in this case since we can't know the user
    if (egressUser.isEmpty()) {
      logger.warning(
          String.format(
              "An egress event happened for workspace: %s, but we're unable to find a user for it. "
                  + "This could be because it was triggered by a GKE up that is currently not running.",
              workspaceNamespace));
      this.egressEventAuditor.fireEgressEvent(event);
    }

    logger.warning(
        String.format(
            "Received an egress event from workspace namespace %s, googleProject %s (%.2fMiB, username %s)",
            workspaceNamespace,
            event.getProjectName(),
            event.getEgressMib(),
            egressUser.get().getUsername()));
    this.egressEventAuditor.fireEgressEventForUser(event, egressUser.get());

    Optional<DbEgressEvent> maybeEvent =
        this.maybePersistSumoEgressEvent(event, Optional.of(egressUser.get()), dbWorkspaceMaybe);
    maybeEvent.ifPresent(e -> taskQueueService.pushEgressEventTask(e.getEgressEventId(), false));
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
    this.egressEventAuditor.fireVwbEgressEvent(egressEvent, egressUser.get());
    DbEgressEvent event =
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(egressUser.orElse(null))
                .setVwbEgressEventId(egressEvent.getVwbEgressEventId())
                .setVwbWorkspaceId(egressEvent.getVwbWorkspaceId())
                .setVwbVmName(egressEvent.getVmName())
                .setGcpProjectId(egressEvent.getGcpProjectId())
                .setVwbIncidentCount(egressEvent.getIncidentCount().intValue())
                .setEgressMegabytes(
                    Optional.ofNullable(egressEvent.getEgressMib())
                        // Mebibytes (2^20 bytes) -> Megabytes (10^6 bytes)
                        .map(mib -> (float) (mib * ((1 << 20) / 1e6)))
                        .orElse(null))
                .setEgressWindowSeconds(
                    Optional.ofNullable(egressEvent.getTimeWindowDuration()).orElse(null))
                .setStatus(DbEgressEventStatus.PENDING));
    taskQueueService.pushEgressEventTask(event.getEgressEventId(), true);
  }

  @NotNull
  private Optional<DbUser> findEgressUsers(SumologicEgressEvent event) {

    // This case is when the Egress is from a GCE cluster.
    if (StringUtils.isNotEmpty(event.getVmPrefix())
        && event.getVmPrefix().startsWith("all-of-us")) {
      return gceVmNameToUserDatabaseId(event.getVmPrefix()).flatMap(userService::getByDatabaseId);
    }
    String serviceName = event.getSrcGkeServiceName();
    // This is the GKE case
    if (StringUtils.isNotEmpty(serviceName)) {
      return gkeServiceNameToUserDatabaseId(serviceName).flatMap(userService::getByDatabaseId);
    }
    return Optional.empty();
  }

  private boolean isEventStale(SumologicEgressEvent egressEvent) {
    if (egressEvent.getTimeWindowDuration() == null || egressEvent.getTimeWindowStart() == null) {
      logger.warning("egress event is missing window details, cannot determine staleness");
      return false;
    }

    // For shorter alerting windows, we don't make any claims about staleness. This ensures that we
    // don't misinterpret a delay in when Sumologic runs the query, and when our system receives the
    // event as a stale event.
    final Duration windowDuration = Duration.ofSeconds(egressEvent.getTimeWindowDuration());
    if (windowDuration.getSeconds() < MAXIMUM_EXPECTED_EVENT_AGE.getSeconds()) {
      return false;
    }
    // Anything which isn't from the most recent alerting window is considered stale. Restated
    // differently: stale if more than 2 windows have elapsed since the alert window start.
    final Instant windowStart = Instant.ofEpochMilli(egressEvent.getTimeWindowStart());
    return windowStart.isBefore(clock.instant().minus(windowDuration.multipliedBy(2L)));
  }

  private Optional<DbEgressEvent> maybePersistSumoEgressEvent(
      SumologicEgressEvent event,
      Optional<DbUser> userMaybe,
      Optional<DbWorkspace> workspaceMaybe) {
    if (isEventStaleAndAlreadyPersisted(event, userMaybe, workspaceMaybe)) {
      return Optional.empty();
    }

    return Optional.of(
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(userMaybe.orElse(null))
                .setWorkspace(workspaceMaybe.orElse(null))
                .setEgressMegabytes(
                    Optional.ofNullable(event.getEgressMib())
                        // Mebibytes (2^20 bytes) -> Megabytes (10^6 bytes)
                        .map(mib -> (float) (mib * ((1 << 20) / 1e6)))
                        .orElse(null))
                .setEgressWindowSeconds(
                    Optional.ofNullable(event.getTimeWindowDuration()).orElse(null))
                .setStatus(DbEgressEventStatus.PENDING)
                .setSumologicEvent(new Gson().toJson(event))));
  }

  private boolean isEventStaleAndAlreadyPersisted(
      SumologicEgressEvent event,
      Optional<DbUser> userMaybe,
      Optional<DbWorkspace> workspaceMaybe) {
    if (event.getTimeWindowDuration() == null || userMaybe.isEmpty() || workspaceMaybe.isEmpty()) {
      return false;
    }
    if (!isEventStale(event)) {
      return false;
    }

    // We'll consider this a duplicate a persisted event if it was captured within 2 alert windows
    // of now.
    Duration window = Duration.ofSeconds(event.getTimeWindowDuration());
    Timestamp lookbackLimit = Timestamp.from(clock.instant().minus(window.multipliedBy(2L)));
    List<DbEgressEvent> matchingEvents =
        egressEventDao.findAllByUserAndWorkspaceAndEgressWindowSecondsAndCreationTimeGreaterThan(
            userMaybe.get(), workspaceMaybe.get(), event.getTimeWindowDuration(), lookbackLimit);

    return !matchingEvents.isEmpty();
  }
}
