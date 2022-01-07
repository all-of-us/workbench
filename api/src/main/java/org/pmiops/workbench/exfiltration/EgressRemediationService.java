package org.pmiops.workbench.exfiltration;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.mail.MessagingException;
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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.jira.JiraContent;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.JiraService.IssueProperty;
import org.pmiops.workbench.jira.JiraService.IssueType;
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.jira.model.IssueBean;
import org.pmiops.workbench.jira.model.SearchResults;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.stereotype.Service;

/** Service for automated egress alert remediation. */
@Service
public class EgressRemediationService {
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
  private final MailService mailService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final EgressEventAuditor egressEventAuditor;
  private final EgressEventDao egressEventDao;
  private final JiraService jiraService;

  public EgressRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      MailService mailService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao,
      JiraService jiraService) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.mailService = mailService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.egressEventAuditor = egressEventAuditor;
    this.egressEventDao = egressEventDao;
    this.jiraService = jiraService;
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

    // Gather/merge past alerts into an incident history
    int egressIncidentCount = getEgressIncidentCountForUser(user);

    // Determine escalating remediation action, if any
    EgressAlertRemediationPolicy egressPolicy =
        workbenchConfigProvider.get().egressAlertRemediationPolicy;
    Optional<Escalation> escalation = matchEscalation(egressPolicy, egressIncidentCount);

    // Execute the action, if any
    escalation.ifPresent(
        e -> {
          EgressRemediationAction action;
          if (e.disableUser != null) {
            disableUser(user);
            action = EgressRemediationAction.DISABLE_USER;
          } else if (e.suspendCompute != null) {
            suspendUserCompute(user, Duration.ofMinutes(e.suspendCompute.durationMinutes));
            action = EgressRemediationAction.SUSPEND_COMPUTE;
          } else {
            throw new ServerErrorException("egress alert policy is invalid: " + e);
          }

          if (egressPolicy != null && egressPolicy.enableJiraTicketing) {
            try {
              logEventToJira(event, action);
            } catch (ApiException ex) {
              throw new ServerErrorException("failed to log event to Jira", ex);
            }
          }

          if (shouldNotifyForEvent(event)) {
            try {
              mailService.sendEgressRemediationEmail(user, action);
            } catch (MessagingException ex) {
              throw new ServerErrorException("failed to send egress remediation email", ex);
            }
          }
        });

    egressEventDao.save(event.setStatus(DbEgressEventStatus.REMEDIATED));

    egressEventAuditor.fireRemediateEgressEvent(event, escalation.orElse(null));
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
        egressEventDao.findAllByUserAndStatusNot(user, DbEgressEventStatus.VERIFIED_FALSE_POSITIVE);

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
    userService.updateUserWithRetries(
        u -> {
          u.setDisabled(true);
          return u;
        },
        user,
        Agent.asSystem());

    // also stop any running compute, killing any active egress processes the user may have
    stopUserRuntimes(user.getUsername());
  }

  private void stopUserRuntimes(String userEmail) {
    int stopCount = leonardoNotebooksClient.stopAllUserRuntimesAsService(userEmail);
    log.info(String.format("stopped %d runtimes for user", stopCount));
  }

  /**
   * File a Jira investigation ticket if no open ticket exists for this VM. Identify matching
   * tickets by the "Egress VM Prefix" and "RW Environment" custom fields on the ticket. If a ticket
   * already exists, add a comment instead.
   */
  private void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {
    String envShortName = workbenchConfigProvider.get().server.shortName;
    SearchResults results =
        jiraService.searchIssues(
            // Ideally we would use Resolution = Unresolved here, but due to a misconfiguration of
            // RW Jira, transitioning to Won't Fix / Duplicate do not currently resolve an issue.
            String.format(
                "\"%s\" ~ \"%s\""
                    + " AND \"%s\" ~ \"%s\""
                    + " AND status not in (Done, \"Won't Fix\", Duplicate)"
                    + " ORDER BY created DESC",
                IssueProperty.EGRESS_VM_PREFIX.key(),
                event.getUser().getRuntimeName(),
                IssueProperty.RW_ENVIRONMENT.key(),
                envShortName));

    if (results.getIssues().isEmpty()) {
      CreatedIssue createdIssue =
          jiraService.createIssue(
              IssueType.TASK,
              JiraContent.contentAsMinimalAtlassianDocument(jiraEventDescription(event, action)),
              ImmutableMap.<IssueProperty, Object>builder()
                  .put(
                      IssueProperty.SUMMARY,
                      String.format(
                          "(%s) Investigate egress from %s",
                          JiraService.summaryDateFormat.format(clock.instant()),
                          event.getUser().getUsername()))
                  .put(IssueProperty.EGRESS_VM_PREFIX, event.getUser().getRuntimeName())
                  .put(IssueProperty.RW_ENVIRONMENT, envShortName)
                  .put(IssueProperty.LABELS, new String[] {"high-egress"})
                  .build());
      log.info("created new egress Jira ticket: " + createdIssue.getKey());
    } else {
      IssueBean existingIssue = results.getIssues().get(0);
      if (results.getIssues().size() > 1) {
        log.warning(
            String.format(
                "found multiple (%d) open Jira tickets for the same user VM prefix, updating the most recent ticket",
                results.getIssues().size()));
      }
      jiraService.commentIssue(
          existingIssue.getId(),
          JiraContent.contentAsMinimalAtlassianDocument(jiraEventComment(event, action)));
      log.info("commented on existing egress Jira ticket: " + existingIssue.getKey());
    }
  }

  private Stream<AtlassianContent> jiraEventDescription(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbUser> user = Optional.ofNullable(event.getUser());
    WorkbenchConfig config = workbenchConfigProvider.get();
    SumologicEgressEvent originalEvent =
        new Gson().fromJson(event.getSumologicEvent(), SumologicEgressEvent.class);
    return Stream.concat(
        Stream.of(
            JiraContent.text(
                String.format("Notebook server VM prefix: %s\n", originalEvent.getVmPrefix())),
            JiraContent.text(
                String.format(
                    "User running notebook: %s\n",
                    user.map(DbUser::getUsername).orElse("unknown"))),
            JiraContent.text("User admin console (as workbench admin user): "),
            user.map(
                    u ->
                        JiraContent.link(
                            String.format(
                                config.server.uiBaseUrl
                                    + "/admin/users/"
                                    + u.getUsername()
                                        .replaceFirst(
                                            "@" + config.googleDirectoryService.gSuiteDomain, ""))))
                .orElse(JiraContent.text("unknown")),
            JiraContent.text("\n\n")),
        jiraEventDescriptionShort(event, action));
  }

  private Stream<AtlassianContent> jiraEventComment(
      DbEgressEvent event, EgressRemediationAction action) {
    return Stream.concat(
        Stream.of(JiraContent.text("Additional egress detected\n\n")),
        jiraEventDescriptionShort(event, action));
  }

  private Stream<AtlassianContent> jiraEventDescriptionShort(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbWorkspace> workspace = Optional.ofNullable(event.getWorkspace());
    SumologicEgressEvent originalEvent =
        new Gson().fromJson(event.getSumologicEvent(), SumologicEgressEvent.class);
    return Stream.of(
        JiraContent.text(String.format("Egress event details (as RW admin): ", action)),
        JiraContent.link(
            workbenchConfigProvider.get().server.uiBaseUrl
                + "/admin/egress-events/"
                + event.getEgressEventId()),
        JiraContent.text(String.format("\n\nAction taken: %s\n\n", action)),
        JiraContent.text(
            String.format(
                "Terra billing project / workspace namespace: %s\n",
                workspace.map(DbWorkspace::getWorkspaceNamespace).orElse("unknown"))),
        JiraContent.text(
            String.format("Google project ID: %s\n\n", originalEvent.getProjectName())),
        JiraContent.text(
            String.format(
                "Detected @ %s\n",
                JiraService.detailedDateFormat.format(
                    Instant.ofEpochMilli(originalEvent.getTimeWindowStart())))),
        JiraContent.text(
            String.format(
                "Total egress detected: %.2f MiB in %d secs\n",
                originalEvent.getEgressMib(), originalEvent.getTimeWindowDuration())),
        JiraContent.text(
            String.format(
                "Egress breakdown: GCE - %.2f MiB, Dataproc - %.2fMiB via master, %.2fMiB via workers\n\n",
                originalEvent.getGceEgressMib(),
                originalEvent.getDataprocMasterEgressMib(),
                originalEvent.getDataprocWorkerEgressMib())),
        JiraContent.text("Workspace admin console (as RW admin):"),
        workspace
            .map(
                w ->
                    JiraContent.link(
                        workbenchConfigProvider.get().server.uiBaseUrl
                            + "/admin/workspaces/"
                            + w.getWorkspaceNamespace()))
            .orElse(JiraContent.text("unknown")));
  }
}
