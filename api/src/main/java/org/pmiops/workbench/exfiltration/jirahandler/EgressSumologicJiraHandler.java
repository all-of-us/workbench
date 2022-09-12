package org.pmiops.workbench.exfiltration.jirahandler;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.jira.JiraContent;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.JiraService.IssueProperty;
import org.pmiops.workbench.jira.JiraService.IssueType;
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.jira.model.IssueBean;
import org.pmiops.workbench.jira.model.SearchResults;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.utils.mappers.EgressEventMapper;
import org.springframework.stereotype.Service;

@Service("sumologic-jira-handler")
public class EgressSumologicJiraHandler implements EgressJiraHandler {

  private static final Logger log = Logger.getLogger(EgressSumologicJiraHandler.class.getName());

  private final Clock clock;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final EgressEventMapper egressEventMapper;
  private final JiraService jiraService;

  public EgressSumologicJiraHandler(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      EgressEventMapper egressEventMapper,
      JiraService jiraService) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.egressEventMapper = egressEventMapper;
    this.jiraService = jiraService;
  }

  /**
   * File a Jira investigation ticket if no open ticket exists for this VM. Identify matching
   * tickets by the "Egress VM Prefix" and "RW Environment" custom fields on the ticket. If a ticket
   * already exists, add a comment instead.
   */
  public void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
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
    SumologicEgressEvent originalEvent = egressEventMapper.toSumoLogicEvent(event);
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
    SumologicEgressEvent originalEvent = egressEventMapper.toSumoLogicEvent(event);
    EgressEvent apiEvent = egressEventMapper.toApiEvent(event);
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
                "Detected between %s and %s\n",
                JiraService.detailedDateFormat.format(
                    Instant.ofEpochMilli(apiEvent.getTimeWindowStartEpochMillis())),
                JiraService.detailedDateFormat.format(
                    Instant.ofEpochMilli(apiEvent.getTimeWindowEndEpochMillis())))),
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
