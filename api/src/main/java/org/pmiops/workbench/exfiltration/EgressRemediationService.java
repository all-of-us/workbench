package org.pmiops.workbench.exfiltration;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.EgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.stereotype.Service;

/** Service for automated egress alert remediation. */
@Service
public class EgressRemediationService {
  // Heuristic window for merging egress alerts into an "incident", for characterizing prior user
  // behavior.
  private static final Duration EGRESS_INCIDENT_MERGE_WINDOW = Duration.ofMinutes(90L);

  private static final Logger log = Logger.getLogger(EgressRemediationService.class.getName());

  private final Clock clock;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final EgressEventDao egressEventDao;

  public EgressRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      EgressEventDao egressEventDao) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.egressEventDao = egressEventDao;
  }

  public void remediateEgressEvent(long egressEventId) {
    // TODO(RW-7389): Add audit logging of remediation.

    DbEgressEvent event =
        egressEventDao
            .findById(egressEventId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("egress event %d does not exist", egressEventId)));
    if (!EgressEventStatus.PENDING.equals(event.getStatus())) {
      log.warning(
          String.format("ignoring non-PENDING egress event %d, nothing to do", egressEventId));
      return;
    }

    DbUser user = event.getUser();
    if (user == null) {
      throw new FailedPreconditionException(
          String.format(
              "egress event %d has no associated user, please investigate", egressEventId));
    }

    // Gather/merge past alerts into an incident history
    int egressIncidentCount = getEgressIncidentCountForUser(user);

    // Determine escalating remediation action, if any
    Optional<Escalation> escalation =
        matchEscalation(
            workbenchConfigProvider.get().egressAlertRemediationPolicy, egressIncidentCount);

    // Execute the action, if any
    escalation.ifPresent(
        e -> {
          if (e.disableUser != null) {
            disableUser(user);
          } else if (e.suspendCompute != null) {
            suspendUserCompute(user, Duration.ofMinutes(e.suspendCompute.durationMinutes));
          } else {
            throw new ServerErrorException("egress alert policy is invalid: " + e);
          }
        });

    egressEventDao.save(event.setStatus(EgressEventStatus.REMEDIATED));
  }

  /**
   * Returns a heuristic count of logical egress "incidents", which are created by merging proximal
   * egress alerts with each-other. A logical count of incidents can be used to characterize prior
   * user behavior. For example, we can avoid over-escalating the remediation for a user who might
   * have tripped multiple alerting thresholds during the same effective incident. Alerts which are
   * verified as false positives do *not* contribute to this count. Alerts from different workspaces
   * (and therefore different runtimes) are never merged.
   *
   * @return the count of logical egress incidents for this user for all time, including any events
   *     which are actively being processed
   */
  private int getEgressIncidentCountForUser(DbUser user) {
    List<DbEgressEvent> events =
        egressEventDao.findAllByUserAndStatusNot(user, EgressEventStatus.VERIFIED_FALSE_POSITIVE);

    // If any egress alerts are missing workspace metadata (this should not happen), consider each
    // as unique incidents.
    int incidentCount = (int) events.stream().filter(e -> e.getWorkspace() == null).count();

    // Stratify the other events by workspace ID, a single incident cannot span multiple workspaces.
    Collection<List<DbEgressEvent>> eventsByWorkspace =
        events.stream()
            .filter(e -> e.getWorkspace() != null)
            .collect(Collectors.groupingBy(e -> e.getWorkspace().getWorkspaceId()))
            .values();

    for (List<DbEgressEvent> partition : eventsByWorkspace) {
      List<Instant> sortedEventCreationTimes =
          partition.stream()
              .map(e -> e.getCreationTime().toInstant())
              .sorted()
              .collect(Collectors.toList());

      // Note: partitions cannot be empty, per above workspace partitioning.
      Instant lastGroupStart = sortedEventCreationTimes.get(0);
      incidentCount++;

      for (Instant eventCreationTime : sortedEventCreationTimes) {
        // If we receive multiple events for the same user/workspace within a short duration, group
        // these together for the purposes of incident history analysis.
        if (eventCreationTime.isAfter(lastGroupStart.plus(EGRESS_INCIDENT_MERGE_WINDOW))) {
          incidentCount++;
          lastGroupStart = eventCreationTime;
        }
      }
    }

    return incidentCount;
  }

  private static Optional<Escalation> matchEscalation(
      @Nullable EgressAlertRemediationPolicy policy, int incidentCount) {
    if (policy == null) {
      return Optional.empty();
    }
    List<Escalation> descendingEscalations =
        policy.escalations.stream()
            .sorted(Comparator.comparingInt((Escalation e) -> e.afterIncidentCount).reversed())
            .collect(Collectors.toList());
    for (Escalation e : descendingEscalations) {
      // We validate against duplicate afterIncidentCount's upstream. If somehow we still have
      // duplicates at this point, we pick the last matching escalation, as the above sort is
      // stable.
      if (e.afterIncidentCount <= incidentCount) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  private void suspendUserCompute(DbUser user, Duration duration) {
    Timestamp suspendUntil = Timestamp.from(clock.instant().plus(duration));
    userService.updateUserWithRetries(
        u -> {
          // Prevent the user from interacting with compute in Workbench
          u.setComputeSecuritySuspendedUntil(suspendUntil);
          return u;
        },
        user,
        Agent.asSystem());

    stopUserRuntimes(user.getUsername());
  }

  private void disableUser(DbUser user) {
    userService.setDisabledStatus(user.getUserId(), true);

    // also stop any running compute, killing any active egress processes the user may have
    stopUserRuntimes(user.getUsername());
  }

  private void stopUserRuntimes(String userEmail) {
    int stopCount = leonardoNotebooksClient.stopAllUserRuntimesAsService(userEmail);
    log.info(String.format("stopped %d runtimes for user", stopCount));
  }
}
