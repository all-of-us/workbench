package org.pmiops.workbench.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

@Configuration
@Profile("local")
public class BigQueryConfigLocal {

    private static final Logger log = Logger.getLogger(BigQueryConfigLocal.class.getName());

    @Bean
    public BigQuery bigQueryService() throws Exception {
        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(new File("sa-key.json")));

        return BigQueryOptions.newBuilder()
                .setProjectId("all-of-us-workbench-test")
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
