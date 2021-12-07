package org.pmiops.workbench.exfiltration;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
import org.pmiops.workbench.utils.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressEventServiceImpl implements EgressEventService {

  private static final Logger logger = Logger.getLogger(EgressEventServiceImpl.class.getName());
  private static final Pattern VM_PREFIX_PATTERN = Pattern.compile("all-of-us-(?<userid>\\d+)");
  private static final String USER_ID_GROUP_NAME = "userid";
  private static final Duration MAXIMUM_EXPECTED_EVENT_AGE = Duration.ofMinutes(5);
  private final Clock clock;
  private final EgressEventAuditor egressEventAuditor;
  private final UserService userService;
  private final TaskQueueService taskQueueService;
  private final WorkspaceDao workspaceDao;
  private final EgressEventDao egressEventDao;

  // Workspace namespace placeholder in Egress alert when workspace namespace is missing.
  // It may happens when workspace is deleted from db, or we are not able to retrieve workspace
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
    if (!dbWorkspaceMaybe.isPresent()) {
      logger.warning(
          String.format(
              "Workspace not found by given Google Project Id: %s", event.getProjectName()));
      workspaceNamespace = NOT_FOUND_WORKSPACE_NAMESPACE;
    } else {
      workspaceNamespace = dbWorkspaceMaybe.get().getWorkspaceNamespace();
    }

    Optional<DbUser> dbUserMaybe =
        vmNameToUserDatabaseId(event.getVmPrefix()).flatMap(userService::getByDatabaseId);
    if (!dbUserMaybe.isPresent()) {
      logger.warning(String.format("user not found by given VM prefix: %s", event.getVmPrefix()));
    }

    logger.warning(
        String.format(
            "Received an egress event from workspace namespace %s, googleProject %s (%.2fMiB, VM prefix %s)",
            workspaceNamespace, event.getProjectName(), event.getEgressMib(), event.getVmPrefix()));
    this.egressEventAuditor.fireEgressEvent(event);

    Optional<DbEgressEvent> maybeEvent =
        this.maybePersistEgressEvent(event, dbUserMaybe, dbWorkspaceMaybe);
    maybeEvent.ifPresent(e -> taskQueueService.pushEgressEventTask(e.getEgressEventId()));
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

  private Optional<DbEgressEvent> maybePersistEgressEvent(
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
    if (event.getTimeWindowDuration() == null
        || !userMaybe.isPresent()
        || !workspaceMaybe.isPresent()) {
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

  private Optional<Long> vmNameToUserDatabaseId(String vmPrefix) {
    return Matchers.getGroup(VM_PREFIX_PATTERN, vmPrefix, USER_ID_GROUP_NAME).map(Long::parseLong);
  }
}
