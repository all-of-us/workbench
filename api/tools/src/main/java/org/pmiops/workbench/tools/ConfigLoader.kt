package org.pmiops.workbench.tools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.pmiops.workbench.config.FeaturedWorkspacesConfig
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.ConfigDao
import org.pmiops.workbench.db.model.Config
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
/**
 * Command-line tool to load a WorkbenchConfig or CdrBigQuerySchemaConfig from a local file and
 * store it in the MySQL database for the current environment.
 *
 *
 * Run by api/project.rb update-cloud-config and (locally) docker-compose run api-scripts
 * ./gradlew loadConfig, which is automatically invoked during api/project.rb dev-up.
 */
class ConfigLoader {

    @Bean
    fun run(configDao: ConfigDao): CommandLineRunner {
        return { args ->
            require(args.size == 2) { "Expected arguments: <config key> <config file>" }
            val configKey = args[0]
            val configFile = args[1]

            val configClass = CONFIG_CLASS_MAP[configKey]
                    ?: throw IllegalArgumentException("Unrecognized config key: $configKey")

            val jackson = ObjectMapper()
            val rawJson = String(Files.readAllBytes(Paths.get(configFile)), Charset.defaultCharset())
            // Strip all lines starting with '//'.
            val strippedJson = rawJson.replace("\\s*//.*".toRegex(), "")
            val newJson = jackson.readTree(strippedJson)

            // Make sure the config parses to the appropriate configuration format,
            // and has the same representation after being marshalled back to JSON.
            val gson = Gson()
            val configObj = gson.fromJson<*>(newJson.toString(), configClass)
            val marshalledJson = gson.toJson(configObj, configClass)
            val marshalledNode = jackson.readTree(marshalledJson)
            val marshalledDiff = JsonDiff.asJson(newJson, marshalledNode)
            if (marshalledDiff.size() > 0) {
                log.info(
                        String.format(
                                "Configuration doesn't match {0} format; see diff.", configClass.simpleName))
                log.info(marshalledDiff.toString())
                System.exit(1)
            }
            val existingConfig = configDao.findOne(configKey)
            if (existingConfig == null) {
                log.info("No configuration exists, creating one.")
                val config = Config()
                config.configId = configKey
                config.configuration = newJson.toString()
                configDao.save(config)
            } else {
                val existingJson = jackson.readTree(existingConfig.configuration)
                val diff = JsonDiff.asJson(existingJson, newJson)
                if (diff.size() == 0) {
                    log.info("No change in configuration; exiting.")
                } else {
                    log.info("Updating configuration:")
                    log.info(diff.toString())
                    existingConfig.configuration = newJson.toString()
                    configDao.save(existingConfig)
                }
            }
            log.info("Done.")
        }
    }

    companion object {

        private val log = Logger.getLogger(ConfigLoader::class.java.name)

        private val CONFIG_CLASS_MAP = ImmutableMap.of(
                Config.MAIN_CONFIG_ID,
                WorkbenchConfig::class.java,
                Config.CDR_BIGQUERY_SCHEMA_CONFIG_ID,
                CdrBigQuerySchemaConfig::class.java,
                Config.FEATURED_WORKSPACES_CONFIG_ID,
                FeaturedWorkspacesConfig::class.java)

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(ConfigLoader::class.java).web(false).run(*args)
        }
    }
}
