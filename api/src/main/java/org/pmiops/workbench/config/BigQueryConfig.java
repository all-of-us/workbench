package org.pmiops.workbench.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryConfig {

  @Bean
  public BigQuery bigQuery() {
    return BigQueryOptions.newBuilder().setProjectId("fc-aou-cdr-synthetic-test").build().getService();
  }
}
