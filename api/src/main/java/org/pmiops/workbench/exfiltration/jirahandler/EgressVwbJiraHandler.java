package org.pmiops.workbench.exfiltration.jirahandler;

import jakarta.inject.Provider;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.exfiltration.ExfiltrationUtils;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.jira.JiraContent;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.SearchResults;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.utils.mappers.VwbEgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(ExfiltrationUtils.EGRESS_VWB_JIRA_HANDLER_QUALIFIER)
public class EgressVwbJiraHandler extends EgressJiraHandler {

  private static final Logger log = Logger.getLogger(EgressVwbJiraHandler.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final VwbEgressEventMapper vwbEgressEventMapper;

  @Autowired
  public EgressVwbJiraHandler(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      VwbEgressEventMapper vwbEgressEventMapper) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.vwbEgressEventMapper = vwbEgressEventMapper;
  }

  /**
   * File a Jira investigation ticket if no open ticket exists for this VM. Identify matching
   * tickets by the "Egress VM Prefix" and "RW Environment" custom fields on the ticket. If a ticket
   * already exists, add a comment instead.
   */
  public void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {
    String envShortName = workbenchConfigProvider.get().server.shortName;
    SearchResults results = searchJiraIssuesWithLabel(event, envShortName, "high-egress");
    log.info(
        String.format("Found %d jira issues with label: high-egress", results.getIssues().size()));
    if (results.getIssues().isEmpty()) {
      createJiraIssueWithLabels(event, action, envShortName, new String[] {"high-egress", "vwb"});
    } else {
      createJiraComment(event, action, results);
    }
  }

  @Override
  protected Stream<AtlassianContent> jiraEventDescription(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbUser> user = Optional.ofNullable(event.getUser());
    WorkbenchConfig config = workbenchConfigProvider.get();
    return Stream.concat(
        Stream.of(
            JiraContent.text(String.format("VM name: %s\n", event.getVwbVmName())),
            JiraContent.text(
                String.format(
                    "User running the app/runtime: %s\n",
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

  @Override
  Stream<AtlassianContent> jiraEventComment(DbEgressEvent event, EgressRemediationAction action) {
    return Stream.concat(
        Stream.of(JiraContent.text("Additional egress detected\n\n")),
        jiraEventDescriptionShort(event, action));
  }

  private Stream<AtlassianContent> jiraEventDescriptionShort(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbWorkspace> workspace = Optional.ofNullable(event.getWorkspace());
    EgressEvent apiEvent = vwbEgressEventMapper.toApiEvent(event);
    return Stream.of(
        JiraContent.text(String.format("Egress event details (as RW admin): ", action)),
        JiraContent.link(
            workbenchConfigProvider.get().server.uiBaseUrl
                + "/admin/egress-events/"
                + event.getEgressEventId()),
        JiraContent.text(String.format("\n\nAction taken: %s\n\n", action)),
        JiraContent.text(
            String.format("Verily Workbench workspace ID: %s\n", event.getVwbEgressEventId())),
        JiraContent.text(String.format("Google project ID: %s\n\n", event.getGcpProjectId())),
        JiraContent.text(
            String.format(
                "Detected between %s and %s\n",
                JiraService.detailedDateFormat.format(
                    Instant.ofEpochMilli(apiEvent.getTimeWindowStartEpochMillis())),
                JiraService.detailedDateFormat.format(
                    Instant.ofEpochMilli(apiEvent.getTimeWindowEndEpochMillis())))),
        JiraContent.text(
            String.format(
                "Total egress detected: %.2f MB in %d secs\n",
                event.getEgressMegabytes(), event.getEgressWindowSeconds())),
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
