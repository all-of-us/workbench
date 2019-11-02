package org.pmiops.workbench.tools

import com.google.common.base.Joiner
import com.google.common.collect.Maps
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileReader
import java.io.IOException
import java.sql.Timestamp
import java.util.Arrays
import java.util.HashSet
import java.util.logging.Logger
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.model.ArchivalStatus
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * See api/project.rb update-cdr-versions. Reads CDR versions from a JSON file and updates the
 * database to match.
 */
@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
class UpdateCdrVersions {

    @Bean
    @Throws(IOException::class)
    fun run(cdrVersionDao: CdrVersionDao, workspaceDao: WorkspaceDao): CommandLineRunner {
        return { args ->
            require(args.size == 2) { "Expected 2 args (file, dry_run). Got " + Arrays.asList<String>(*args) }
            val dryRun = java.lang.Boolean.parseBoolean(args[1])
            val cdrVersionsReader = FileReader(args[0])
            val gson = GsonBuilder()
                    .registerTypeAdapter(Timestamp::class.java, TimestampGsonAdapter())
                    .create()
            val cdrVersions = gson.fromJson<List<CdrVersion>>(cdrVersionsReader, object : TypeToken<List<CdrVersion>>() {

            }.type)

            val ids = HashSet<Long>()
            val defaultIds = HashSet<Long>()
            for (v in cdrVersions) {
                require(v.getCdrVersionId() != 0L) { String.format("Input JSON CDR version '%s' is missing an ID", v.getName()) }
                require(ids.add(v.getCdrVersionId())) {
                    String.format(
                            "Input JSON contains duplicated CDR version ID %d", v.getCdrVersionId())
                }
                if (v.getIsDefault()) {
                    require(!(ArchivalStatus.LIVE !== v.getArchivalStatusEnum())) {
                        String.format(
                                "Archived CDR version cannot also be the default", v.getCdrVersionId())
                    }
                    defaultIds.add(v.getCdrVersionId())
                }
            }
            require(defaultIds.size == 1) {
                String.format(
                        "Must be exactly one default CDR version, got %d: %s",
                        defaultIds.size, Joiner.on(", ").join(defaultIds))
            }

            val currentCdrVersions = Maps.newHashMap<Long, CdrVersion>()
            for (cdrVersion in cdrVersionDao.findAll()) {
                currentCdrVersions[cdrVersion.cdrVersionId] = cdrVersion
            }
            val dryRunSuffix = if (dryRun) " (dry run)" else ""
            for (cdrVersion in cdrVersions) {
                val existingCdrVersion = currentCdrVersions.remove(cdrVersion.getCdrVersionId())
                if (existingCdrVersion == null) {
                    logger.info(
                            String.format(
                                    "Inserting new CDR version %d '%s'%s: %s",
                                    cdrVersion.getCdrVersionId(),
                                    cdrVersion.getName(),
                                    dryRunSuffix,
                                    gson.toJson(cdrVersion)))
                    if (!dryRun) {
                        cdrVersionDao.save<CdrVersion>(cdrVersion)
                    }
                } else {
                    if (cdrVersion == existingCdrVersion) {
                        logger.info(
                                String.format(
                                        "CDR version %d '%s' unchanged.",
                                        cdrVersion.getCdrVersionId(), cdrVersion.getName()))
                    } else {
                        logger.info(
                                String.format(
                                        "Updating CDR version %d '%s'%s: %s",
                                        cdrVersion.getCdrVersionId(),
                                        cdrVersion.getName(),
                                        dryRunSuffix,
                                        gson.toJson(cdrVersion)))
                        if (!dryRun) {
                            cdrVersionDao.save<CdrVersion>(cdrVersion)
                        }
                    }
                }
            }
            for (cdrVersion in currentCdrVersions.values) {
                logger.info(
                        String.format(
                                "Deleting CDR version %d '%s' no longer in file%s: %s",
                                cdrVersion.cdrVersionId,
                                cdrVersion.name,
                                dryRunSuffix,
                                gson.toJson(cdrVersion)))
                if (!dryRun) {
                    // Note: this will fail if the database still has references to the CDR version being
                    // deleted.
                    cdrVersionDao.delete(cdrVersion.cdrVersionId)
                }
            }
        }
    }

    companion object {

        private val logger = Logger.getLogger(UpdateCdrVersions::class.java.name)

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(UpdateCdrVersions::class.java).web(false).run(*args)
        }
    }
}
