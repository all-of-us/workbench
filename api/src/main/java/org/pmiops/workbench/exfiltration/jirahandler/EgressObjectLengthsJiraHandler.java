package org.pmiops.workbench.exfiltration.jirahandler;

import static org.pmiops.workbench.exfiltration.ExfiltrationConstants.OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER;

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
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER)
public class EgressObjectLengthsJiraHandler extends EgressJiraHandler {

  private static final Logger log =
      Logger.getLogger(EgressObjectLengthsJiraHandler.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public EgressObjectLengthsJiraHandler(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {
    String envShortName = workbenchConfigProvider.get().server.shortName;
    SearchResults results =
        searchJiraIssuesWithLabel(event, envShortName, "file-length-high-egress");
    log.info(
        String.format(
            "Found %d jira issues with label: file-length-high-egress",
            results.getIssues().size()));
    if (results.getIssues().isEmpty()) {
      createJiraIssueWithLabels(
          event, action, envShortName, new String[] {"file-length-high-egress"});
    } else {
      createJiraComment(event, action, results);
    }
  }

  @Override
  Stream<AtlassianContent> jiraEventDescription(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbUser> user = Optional.ofNullable(event.getUser());
    WorkbenchConfig config = workbenchConfigProvider.get();
    return Stream.concat(
        Stream.of(
            JiraContent.text(
                String.format("Notebook server VM prefix: %s\n", event.getUser().getRuntimeName())),
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

  private Stream<AtlassianContent> jiraEventDescriptionShort(
      DbEgressEvent event, EgressRemediationAction action) {
    Optional<DbWorkspace> workspace = Optional.ofNullable(event.getWorkspace());
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
            String.format("Google project ID: %s\n\n", event.getWorkspace().getGoogleProject())),
        JiraContent.text(
            String.format("Total egress detected: %.2f MiB\n", event.getEgressMegabytes())),
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

  @Override
  Stream<AtlassianContent> jiraEventComment(DbEgressEvent event, EgressRemediationAction action) {
    return Stream.concat(
        Stream.of(JiraContent.text("Additional file length egress detected\n\n")),
        jiraEventDescriptionShort(event, action));
  }
}
