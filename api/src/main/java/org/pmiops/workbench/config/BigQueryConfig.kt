package org.pmiops.workbench.config

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BigQueryConfig {

    @Bean
    fun bigQuery(): BigQuery {
        return BigQueryOptions.getDefaultInstance().service
    }
}
