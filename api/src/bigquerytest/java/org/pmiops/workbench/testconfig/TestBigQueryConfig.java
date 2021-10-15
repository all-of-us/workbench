package org.pmiops.workbench.testconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import org.pmiops.workbench.config.BigQueryConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class TestBigQueryConfig {

  @Bean
  public BigQuery bigQuery() throws Exception {
    InputStream keyStream = new FileInputStream(new File("sa-key.json"));
    GoogleCredentials credentials = ServiceAccountCredentials.fromStream(keyStream);

    return BigQueryOptions.newBuilder()
        .setProjectId("all-of-us-workbench-test")
        .setCredentials(credentials)
        // Note: Later we may want to apply similar settings to the real server. See RW-7413.
        .setRetrySettings(
            RetrySettings.newBuilder()
                .setMaxAttempts(3)
                .setInitialRpcTimeout(org.threeten.bp.Duration.ofSeconds(20L))
                .setInitialRetryDelay(org.threeten.bp.Duration.ofSeconds(1L))
                .build())
        .build()
        .getService();
  }

  @Bean(name = BigQueryConfig.DEFAULT_QUERY_TIMEOUT)
  public Duration defaultQueryTimeout() {
    // We're not subject to GAE timeouts in unit tests. Allow queries to run a bit longer to avoid
    // failing on the occasional slow job.
    return Duration.ofMinutes(3L);
  }

  @Bean
  public TestWorkbenchConfig bqConfig() throws Exception {
    ObjectMapper jackson = new ObjectMapper();
    String dataSetId = "test_" + UUID.randomUUID().toString().replaceAll("-", "_");
    JsonNode newJson =
        jackson.readTree(
            "{\"bigquery\": {\"dataSetId\": \""
                + dataSetId
                + "\",\"projectId\": \"all-of-us-workbench-test\"}}");
    Gson gson = new Gson();
    TestWorkbenchConfig testWorkbenchConfig =
        gson.fromJson(newJson.toString(), TestWorkbenchConfig.class);
    return testWorkbenchConfig;
  }
}
