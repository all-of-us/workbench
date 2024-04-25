package org.pmiops.workbench.jira;

import com.google.common.collect.ImmutableMap;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.inject.Provider;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.AtlassianDocument;
import org.pmiops.workbench.jira.model.Comment;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.jira.model.IssueTypeDetails;
import org.pmiops.workbench.jira.model.IssueUpdateDetails;
import org.pmiops.workbench.jira.model.Project;
import org.pmiops.workbench.jira.model.SearchRequestBean;
import org.pmiops.workbench.jira.model.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to mediate interactions with the RW Jira project.
 *
 * <p>To get the schema for RW issues (including all field names):
 *
 * <pre><code>
 * # Get USER / API KEY from credentials bucket.
 * curl --request GET \
 *   --url "https://precisionmedicineinitiative.atlassian.net/rest/api/3/issue/createmeta?projectKeys=RW&expand=projects.issuetypes.fields" \
 *   --header Accept: application/json \
 *   --user "USER:API_KEY" | jq .
 *  </code>
 */
@Service
public class JiraService {
  private static final String PROJECT_KEY = "RW";
  private static final Logger log = Logger.getLogger(JiraService.class.getName());

  public static final ZoneId displayTimeZone = ZoneId.of("America/Chicago");
  public static final DateTimeFormatter summaryDateFormat =
      DateTimeFormatter.ofPattern("MM/dd/yy").withLocale(Locale.US).withZone(displayTimeZone);
  public static final DateTimeFormatter detailedDateFormat =
      DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a 'Central Time'")
          .withLocale(Locale.US)
          .withZone(displayTimeZone);

  @Autowired private Provider<JiraApi> apiProvider;

  public enum IssueProperty {
    DESCRIPTION("description"),
    EGRESS_VM_PREFIX("Egress VM Prefix[Short text]", "customfield_10795"),
    ISSUE_TYPE("issuetype"),
    LABELS("labels"),
    PROJECT("project"),
    RW_ENVIRONMENT("RW Environment[Short text]", "customfield_10759"),
    SUMMARY("summary");

    private final String key;
    private final String fieldName;

    IssueProperty(String key) {
      this(key, key);
    }

    IssueProperty(String key, String fieldName) {
      this.key = key;
      this.fieldName = fieldName;
    }

    public String key() {
      return key;
    }

    public String fieldName() {
      return fieldName;
    }
  }

  public enum IssueType {
    TASK("Task", "10001"),
    BUG("Bug", "10003"),
    STORY("Story", "10000");

    private final String jiraName;
    private final String jiraId;

    IssueType(String jiraName, String jiraId) {
      this.jiraName = jiraName;
      this.jiraId = jiraId;
    }

    public String jiraName() {
      return jiraName;
    }

    public String jiraId() {
      return jiraId;
    }
  }

  public CreatedIssue createIssue(
      IssueType type, AtlassianDocument description, Map<IssueProperty, Object> issueProps)
      throws ApiException {
    Map<IssueProperty, Object> mergedProps = new HashMap<>(issueProps);
    mergedProps.putAll(
        ImmutableMap.<IssueProperty, Object>builder()
            .put(IssueProperty.PROJECT, new Project().key(PROJECT_KEY))
            .put(IssueProperty.ISSUE_TYPE, new IssueTypeDetails().id(type.jiraId()))
            .put(IssueProperty.DESCRIPTION, description)
            .build());
    try {
      return apiProvider
          .get()
          .createIssue(
              new IssueUpdateDetails()
                  .fields(
                      mergedProps.keySet().stream()
                          .collect(Collectors.toMap(IssueProperty::fieldName, mergedProps::get))),
              false);
    } catch (ApiException e) {
      logJiraErrorPayload(e);
      throw e;
    }
  }

  public void commentIssue(String issueId, AtlassianDocument body) throws ApiException {
    try {
      apiProvider.get().addComment(new Comment().body(body), issueId);
    } catch (ApiException e) {
      logJiraErrorPayload(e);
      throw e;
    }
  }

  public SearchResults searchIssues(String jqlSubquery) throws ApiException {
    try {
      return apiProvider
          .get()
          .searchForIssuesUsingJqlPost(
              new SearchRequestBean()
                  .jql(String.format("project = %s AND %s", PROJECT_KEY, jqlSubquery)));
    } catch (ApiException e) {
      logJiraErrorPayload(e);
      throw e;
    }
  }

  private void logJiraErrorPayload(ApiException e) {
    // Logging at this level and rethrowing is not ideal, but unfortunately the default error
    // message for ApiException completely omits the error message returned from the API, which is
    // found in the response body.
    log.severe("Jira error payload details: " + e.getResponseBody());
  }
}
