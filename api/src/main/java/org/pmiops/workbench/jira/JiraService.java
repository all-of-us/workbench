package org.pmiops.workbench.jira;

import javax.inject.Provider;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.jira.model.EntityProperty;
import org.pmiops.workbench.jira.model.IssueUpdateDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JiraService {
  private static final String PROJECT_KEY = "RW";

  @Autowired private Provider<JiraApi> apiProvider;

  private enum IssueProperties {
    DESCRIPTION("description"),
    ISSUE_TYPE("issuetype"),
    LABELS("labels"),
    PROJECT("project"),
    SUMMARY("summary");

    private final String key;

    IssueProperties(String key) {
      this.key = key;
    }

    String key() {
      return key;
    }
  }

  public enum IssueType {
    TASK("Task"),
    BUG("Bug"),
    STORY("Story");

    private final String value;

    IssueType(String value) {
      this.value = value;
    }

    String value() {
      return value;
    }
  }

  public CreatedIssue createIssue(IssueType type, String summary, String description)
      throws ApiException {

    return apiProvider
        .get()
        .createIssue(
            new IssueUpdateDetails()
                .addPropertiesItem(
                    new EntityProperty().key(IssueProperties.PROJECT.key()).value(PROJECT_KEY))
                .addPropertiesItem(
                    new EntityProperty().key(IssueProperties.ISSUE_TYPE.key()).value(type.value()))
                .addPropertiesItem(
                    new EntityProperty().key(IssueProperties.SUMMARY.key()).value(summary))
                .addPropertiesItem(
                    new EntityProperty().key(IssueProperties.DESCRIPTION.key()).value(description)),
            false);
  }
}
