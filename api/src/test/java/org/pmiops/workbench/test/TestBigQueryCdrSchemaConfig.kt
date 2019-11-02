package org.pmiops.workbench.test

import com.google.gson.Gson
import java.io.FileReader
import java.io.IOException
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestBigQueryCdrSchemaConfig {

    @Bean
    @Throws(IOException::class)
    internal fun provideCdrSchemaConfig(): CdrBigQuerySchemaConfig {
        val gson = Gson()
        return gson.fromJson(FileReader("config/cdm/cdm_5_2.json"), CdrBigQuerySchemaConfig::class.java)
    }
}
