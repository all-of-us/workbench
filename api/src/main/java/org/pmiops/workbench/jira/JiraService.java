package org.pmiops.workbench.jira;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.AtlassianContent;
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
      IssueType type, String description, Map<IssueProperty, Object> issueProps)
      throws ApiException {

    Map<IssueProperty, Object> mergedProps = new HashMap<>(issueProps);
    mergedProps.putAll(
        ImmutableMap.<IssueProperty, Object>builder()
            .put(IssueProperty.PROJECT, new Project().key(PROJECT_KEY))
            .put(IssueProperty.ISSUE_TYPE, new IssueTypeDetails().id(type.jiraId()))
            .put(IssueProperty.DESCRIPTION, textAsMinimalAtlassianDocument(description))
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

  public void commentIssue(String issueId, String body) throws ApiException {
    try {
      apiProvider
          .get()
          .addComment(new Comment().body(textAsMinimalAtlassianDocument(body)), issueId);
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

  /**
   * Several fields in the Jira API expect content in the Atlassian Document format, which is fairly
   * verbose. This method wraps text in a minimal document. See
   * https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/
   */
  private AtlassianDocument textAsMinimalAtlassianDocument(String text) {
    return new AtlassianDocument()
        .type("doc")
        .version(BigDecimal.ONE)
        .addContentItem(
            new AtlassianContent()
                .type("paragraph")
                .addContentItem(new AtlassianContent().type("text").text(text)));
  }

  private void logJiraErrorPayload(ApiException e) {
    // Logging at this level and rethrowing is not ideal, but unfortunately the default error
    // message for ApiException completely omits the error message returned from the API, which is
    // found in the response body.
    log.severe("Jira error payload details: " + e.getResponseBody());
  }
}
