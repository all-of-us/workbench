package org.pmiops.workbench.api.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "beta", "prod"})
public class BigQueryConfig {

    @Bean
    public BigQuery bigQueryService() {
        return new BigQueryOptions
                .DefaultBigqueryFactory()
                .create( BigQueryOptions.getDefaultInstance() );
    }
}
