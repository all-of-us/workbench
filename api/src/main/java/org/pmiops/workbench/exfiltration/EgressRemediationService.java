package org.pmiops.workbench.exfiltration;

import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;
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
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;

/** Service for automated egress alert remediation. */
public abstract class EgressRemediationService {
  // Heuristic window for merging egress alerts into an "incident", for characterizing prior user
  // behavior.
  private static final Duration EGRESS_INCIDENT_MERGE_WINDOW = Duration.ofMinutes(90L);
  // We use similar logic to debounce email notifications to users. Often we'll receive multiple
  // alerts on different windows for the same effective incident; avoid resending emails in this
  // case (still suspend, to be safe).
  private static final Duration EGRESS_NOTIFY_DEBOUNCE_TIME = Duration.ofHours(1L);

  private static final Logger log = Logger.getLogger(EgressRemediationService.class.getName());

  private final Clock clock;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;
  private final LeonardoApiClient leonardoNotebooksClient;
  private final EgressEventAuditor egressEventAuditor;
  private final EgressEventDao egressEventDao;

  public EgressRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoApiClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.egressEventAuditor = egressEventAuditor;
    this.egressEventDao = egressEventDao;
  }

  public void remediateEgressEvent(long egressEventId) {
    DbEgressEvent event =
        egressEventDao
            .findById(egressEventId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("egress event %d does not exist", egressEventId)));
    if (!DbEgressEventStatus.PENDING.equals(event.getStatus())) {
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

    if (shouldSkipEgressEvent(event)) {
      egressEventDao.save(event.setStatus(DbEgressEventStatus.BYPASSED));
      egressEventAuditor.fireRemediateEgressEvent(event, null);
      return;
    }
    // Gather/merge past alerts into an incident history
    int egressIncidentCount = getEgressIncidentCountForUser(event, user);

    // Determine escalating remediation action, if any
    EgressAlertRemediationPolicy egressPolicy =
        workbenchConfigProvider.get().egressAlertRemediationPolicy;
    Optional<Escalation> escalation = matchEscalation(egressPolicy, egressIncidentCount);

    // Execute the action, if any
    escalation.ifPresent(
        e -> {
          EgressRemediationAction action = processEgressRemediationAction(user, e, event);

          if (egressPolicy != null && egressPolicy.enableJiraTicketing) {
            try {
              logEventToJira(event, action);
            } catch (ApiException ex) {
              throw new ServerErrorException("failed to log event to Jira", ex);
            }
          }

          if (shouldNotifyForEvent(event)) {
            try {
              sendEgressRemediationEmail(user, action, event);
            } catch (MessagingException ex) {
              throw new ServerErrorException("failed to send egress remediation email", ex);
            }
          }
        });

    egressEventDao.save(event.setStatus(DbEgressEventStatus.REMEDIATED));

    egressEventAuditor.fireRemediateEgressEvent(event, escalation.orElse(null));
  }

  @NotNull
  private EgressRemediationAction processEgressRemediationAction(
      DbUser user, Escalation e, DbEgressEvent event) {
    if (ExfiltrationUtils.isVwbEgressEvent(event)) {
      if (e.disableUser != null) {
        disableUserVwbDataAccess(user);
        return EgressRemediationAction.DISABLE_USER;
      } else if (e.suspendCompute != null) {
        // Compute will be suspended by VWB.
        return EgressRemediationAction.SUSPEND_COMPUTE;
      }
    } else {
      if (e.disableUser != null) {
        disableUser(user);
        return EgressRemediationAction.DISABLE_USER;
      } else if (e.suspendCompute != null) {
        suspendUserCompute(user, Duration.ofMinutes(e.suspendCompute.durationMinutes));
        return EgressRemediationAction.SUSPEND_COMPUTE;
      }
    }
    throw new ServerErrorException("egress alert policy is invalid: " + e);
  }

  protected abstract void sendEgressRemediationEmail(
      DbUser user, EgressRemediationAction action, DbEgressEvent event) throws MessagingException;

  protected abstract void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException;

  protected abstract boolean shouldSkipEgressEvent(DbEgressEvent event);

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
  protected int getEgressIncidentCountForUser(DbEgressEvent unused, DbUser user) {
    List<DbEgressEvent> events =
        egressEventDao.findAllByUserAndStatusNotIn(
            user,
            List.of(DbEgressEventStatus.VERIFIED_FALSE_POSITIVE, DbEgressEventStatus.BYPASSED));

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
          partition.stream().map(e -> e.getCreationTime().toInstant()).sorted().toList();

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

  private boolean shouldNotifyForEvent(DbEgressEvent event) {
    if (event.getWorkspace() == null) {
      return true;
    }

    Timestamp debounceAfter =
        Timestamp.from(event.getCreationTime().toInstant().minus(EGRESS_NOTIFY_DEBOUNCE_TIME));

    // We use a [closed, open) time interval check on the event which serves two purposes:
    //  1. Excludes the event we are actively processing.
    //  2. In the extremely unlikely event that we receive two egress events at the exact same time,
    //     this logic will result in 2 notifications, rather than 0. Both event processors will miss
    //     the other event in their respective queries.
    //
    // Filtering for REMEDIATED events could also work here, but could result in duplicate
    // notifications when events are processed in close proximity.
    List<DbEgressEvent> priorEvents =
        egressEventDao.findAllByUserAndWorkspaceAndCreationTimeBetweenAndCreationTimeNot(
            event.getUser(),
            event.getWorkspace(),
            debounceAfter,
            event.getCreationTime(),
            event.getCreationTime());
    return priorEvents.isEmpty();
  }

  private static Optional<Escalation> matchEscalation(
      @Nullable EgressAlertRemediationPolicy policy, int incidentCount) {
    if (policy == null) {
      return Optional.empty();
    }
    List<Escalation> descendingEscalations =
        policy.escalations.stream()
            .sorted(Comparator.comparingInt((Escalation e) -> e.afterIncidentCount).reversed())
            .toList();
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

    stopUserRuntimesAndApps(user.getUsername());
  }

  protected void disableUser(DbUser user) {
    userService.updateUserWithRetries(
        u -> {
          u.setDisabled(true);
          return u;
        },
        user,
        Agent.asSystem());

    // also stop any running compute, killing any active egress processes the user may have
    stopUserRuntimesAndApps(user.getUsername());
  }

  protected void disableUserVwbDataAccess(DbUser user) {
    userService.updateUserWithRetries(
        u -> {
          u.setDisabled(true);
          return u;
        },
        user,
        Agent.asSystem());

    // TODO: Lock user access in VWB.
  }

  private void stopUserRuntimesAndApps(String userEmail) {
    int stoppedRuntimeCount = leonardoNotebooksClient.stopAllUserRuntimesAsService(userEmail);
    int stoppedAppCount = leonardoNotebooksClient.deleteUserAppsAsService(userEmail);
    log.info(
        String.format(
            "stopped %d runtimes and %d apps for user %s",
            stoppedRuntimeCount, stoppedAppCount, userEmail));
  }
}
