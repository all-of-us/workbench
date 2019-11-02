package org.pmiops.workbench.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.IOException
import java.io.InputStream
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Optional
import java.util.logging.Logger
import javax.inject.Provider
import javax.persistence.EntityManager
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.DataDictionaryEntry
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench")
@EntityScan("org.pmiops.workbench")
class LoadDataDictionary {

    private val dataDictionaryResources: Array<Resource>
        @Throws(IOException::class)
        get() {
            val cl = this.javaClass.classLoader
            val resolver = PathMatchingResourcePatternResolver(cl)
            return resolver.getResources("classpath*:/data_dictionary_exports/*.yaml")
        }

    @Bean
    fun run(
            cdrVersionDao: CdrVersionDao,
            dataDictionaryEntryDao: DataDictionaryEntryDao,
            entityManager: EntityManager,
            workbenchConfigProvider: Provider<WorkbenchConfig>): CommandLineRunner {
        return { args ->
            require(args.size == 1) { "Expected 1 arg. Got " + Arrays.asList<String>(*args) }

            val dryRun = java.lang.Boolean.parseBoolean(args[0])

            val mapper = ObjectMapper(YAMLFactory())
            val df = SimpleDateFormat("dd-MM-yyyy hh:mm:ss")
            mapper.dateFormat = df
            mapper.registerModule(KotlinModule())

            for (resource in dataDictionaryResources) {
                val `is` = resource.inputStream
                val dd = mapper.readValue(`is`, DataDictionary::class.java)

                val newEntryDefinedTime = dd.meta_data[0].created_time

                val cdrVersion = cdrVersionDao.findByName(dd.meta_data[0].cdr_version)
                        ?: // Skip over Data Dictionaries for CDR Versions not in the current environment
                        continue

                for ((field_name, relevant_omop_table, omop_cdm_standard_or_custom_field, description, field_type, data_provenance, source_ppi_module, transformed_by_registered_tier_privacy_methods) in dd.transformations[0].available_fields) {
                    val entry = dataDictionaryEntryDao.findByRelevantOmopTableAndFieldNameAndCdrVersion(
                            relevant_omop_table, field_name, cdrVersion)

                    // We are skipping ahead if the defined times match by assuming that the definition has
                    // not changed.
                    if (entry.isPresent && newEntryDefinedTime.before(entry.get().definedTime)) {
                        continue
                    }

                    val targetEntry: DataDictionaryEntry
                    if (!entry.isPresent) {
                        targetEntry = DataDictionaryEntry()
                        targetEntry.relevantOmopTable = relevant_omop_table
                        targetEntry.fieldName = field_name
                        targetEntry.cdrVersion = cdrVersion
                    } else {
                        targetEntry = entry.get()
                    }

                    targetEntry.definedTime = newEntryDefinedTime
                    targetEntry.omopCdmStandardOrCustomField = omop_cdm_standard_or_custom_field
                    targetEntry.description = description
                    targetEntry.fieldType = field_type
                    targetEntry.dataProvenance = data_provenance
                    targetEntry.sourcePpiModule = source_ppi_module
                    targetEntry.transformedByRegisteredTierPrivacyMethods = transformed_by_registered_tier_privacy_methods

                    if (dryRun) {
                        logger.info(
                                "Would have saved ("
                                        + targetEntry.relevantOmopTable
                                        + ", "
                                        + targetEntry.fieldName
                                        + ", "
                                        + cdrVersion.name
                                        + ")")
                    } else {
                        dataDictionaryEntryDao.save(targetEntry)
                    }
                }
            }

            val missingCdrVersions = entityManager
                    .createNativeQuery(
                            "select name from cdr_version cdr "
                                    + "left join data_dictionary_entry dde on cdr.cdr_version_id=dde.cdr_version_id "
                                    + "where dde.data_dictionary_entry_id is null")
                    .resultList

            if (workbenchConfigProvider.get().server.projectId == "all-of-us-rw-prod" && missingCdrVersions.size > 0) {
                throw RuntimeException(
                        "No data dictionary found for following CDR Versions: " + missingCdrVersions.joinToString(","))
            }
        }
    }

    companion object {

        private val logger = Logger.getLogger(LoadDataDictionary::class.java.name)

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(LoadDataDictionary::class.java).web(false).run(*args)
        }
    }
}
