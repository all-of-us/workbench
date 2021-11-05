package org.pmiops.workbench.jira;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.jira.api.JiraApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class JiraConfig {
  private static final String JIRA_CREDS_PATH = "jira-login.json";
  private static final String JIRA_CREDS_CACHE = "jiraCredsCache";

  /** Corresponds to jira-login JSON file. */
  private static class JiraCredentials {
    public String username;
    public String apiToken;
  }

  @Bean
  @RequestScope
  public JiraApi jiraApi(ApiClient apiClient) {
    JiraApi api = new JiraApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Lazy
  public ApiClient apiClient(
      @Qualifier(JIRA_CREDS_CACHE) Supplier<JiraCredentials> jiraCredsCache) {
    ApiClient client = new ApiClient();
    JiraCredentials creds = jiraCredsCache.get();
    client.setUsername(creds.username);
    client.setPassword(creds.apiToken);
    return client;
  }

  @Bean
  @Lazy
  @Qualifier(JIRA_CREDS_CACHE)
  Supplier<JiraCredentials> getJiraCredsCache(CloudStorageClient storageClient) {
    return Suppliers.memoizeWithExpiration(
        () ->
            new Gson()
                .fromJson(
                    storageClient.getCredentialsBucketString(JIRA_CREDS_PATH),
                    JiraCredentials.class),
        1,
        TimeUnit.HOURS);
  }
}
