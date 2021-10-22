package org.pmiops.workbench.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryConfig {
  public static final String DEFAULT_QUERY_TIMEOUT = "bigquery-default-query-timeout";

  @Bean
  public BigQuery bigQuery() {
    return BigQueryOptions.getDefaultInstance().getService();
  }

  @Bean(name = DEFAULT_QUERY_TIMEOUT)
  public Duration defaultQueryTimeout() {
    return Duration.ofMinutes(1L);
  }
}
