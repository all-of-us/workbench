package org.pmiops.workbench.exfiltration.jirahandler;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.db.model.DbEgressEvent;
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
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.utils.mappers.EgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public abstract class EgressJiraHandler {

  private static final Logger log = Logger.getLogger(EgressJiraHandler.class.getName());

  @Autowired private Clock clock;
  @Autowired private JiraService jiraService;

  @Autowired private EgressEventMapper egressEventMapper;

  public abstract void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException;

  abstract Stream<AtlassianContent> jiraEventDescription(
      DbEgressEvent event, EgressRemediationAction action);

  abstract Stream<AtlassianContent> jiraEventComment(
      DbEgressEvent event, EgressRemediationAction action);

  protected SearchResults searchJiraIssuesWithLabel(
      DbEgressEvent event, String envShortName, String label) throws ApiException {
    SumologicEgressEvent sumologicEgressEvent = egressEventMapper.toSumoLogicEvent(event);
    String vmName = sumologicEgressEvent.getVmPrefix();
    if (StringUtils.isNotEmpty(sumologicEgressEvent.getSrcGkeCluster())) {
      vmName = sumologicEgressEvent.getVmName();
    }
    return jiraService.searchIssues(
        // Ideally we would use Resolution = Unresolved here, but due to a misconfiguration of
        // RW Jira, transitioning to Won't Fix / Duplicate do not currently resolve an issue.
        String.format(
            "\"%s\" ~ \"%s\""
                + " AND \"%s\" ~ \"%s\""
                + " AND \"%s\" = \"%s\""
                + " AND status not in (Done, \"Won't Fix\", Duplicate)"
                + " ORDER BY created DESC",
            IssueProperty.EGRESS_VM_PREFIX.key(),
            vmName,
            IssueProperty.RW_ENVIRONMENT.key(),
            envShortName,
            IssueProperty.LABELS,
            label));
  }

  protected void createJiraIssueWithLabels(
      DbEgressEvent event, EgressRemediationAction action, String envShortName, String[] labels)
      throws ApiException {
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
                .put(IssueProperty.LABELS, labels)
                .build());
    log.info("created new egress Jira ticket: " + createdIssue.getKey());
  }

  protected void createJiraComment(
      DbEgressEvent event, EgressRemediationAction action, SearchResults results)
      throws ApiException {
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
