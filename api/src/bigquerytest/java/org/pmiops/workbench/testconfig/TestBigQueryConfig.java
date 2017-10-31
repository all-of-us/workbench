package org.pmiops.workbench.testconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.gson.Gson;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
public class TestBigQueryConfig {

    @Bean
    public BigQuery bigQueryService() throws Exception {
        InputStream keyStream =
          new FileInputStream(new File("src/main/webapp/WEB-INF/sa-key.json"));
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(keyStream);

        return BigQueryOptions.newBuilder()
                .setProjectId("all-of-us-workbench-test")
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean
    public WorkbenchConfig workbenchConfig() throws Exception {
        ObjectMapper jackson = new ObjectMapper();
        String dataSetId = "test_" + UUID
                .randomUUID()
                .toString()
                .replaceAll("-", "_");
        JsonNode newJson = jackson
                .readTree("{\"bigquery\": {\"dataSetId\": \"" + dataSetId + "\",\"projectId\": \"all-of-us-workbench-test\"}}");
        Gson gson = new Gson();
        WorkbenchConfig workbenchConfig = gson.fromJson(newJson.toString(), WorkbenchConfig.class);
        return workbenchConfig;
    }
}
