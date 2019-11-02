package org.pmiops.workbench.testconfig

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.context.annotation.Bean

@SpringBootApplication(exclude = [LiquibaseAutoConfiguration::class])
class TestBigQueryConfig {

    @Bean
    @Throws(Exception::class)
    fun bigQuery(): BigQuery {
        val keyStream = FileInputStream(File("sa-key.json"))
        val credentials = ServiceAccountCredentials.fromStream(keyStream)

        return BigQueryOptions.newBuilder()
                .setProjectId("all-of-us-workbench-test")
                .setCredentials(credentials)
                .build()
                .service
    }

    @Bean
    @Throws(Exception::class)
    fun bqConfig(): TestWorkbenchConfig {
        val jackson = ObjectMapper()
        val dataSetId = "test_" + UUID.randomUUID().toString().replace("-".toRegex(), "_")
        val newJson = jackson.readTree(
                "{\"bigquery\": {\"dataSetId\": \""
                        + dataSetId
                        + "\",\"projectId\": \"all-of-us-workbench-test\"}}")
        val gson = Gson()
        return gson.fromJson(newJson.toString(), TestWorkbenchConfig::class.java)
    }
}
