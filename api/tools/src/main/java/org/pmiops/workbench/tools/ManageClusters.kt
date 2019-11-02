package org.pmiops.workbench.tools

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.base.Joiner
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Arrays
import java.util.Comparator
import java.util.HashSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import org.pmiops.workbench.notebooks.ApiClient
import org.pmiops.workbench.notebooks.ApiException
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.model.Cluster
import org.pmiops.workbench.notebooks.model.ClusterStatus
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * ManageClusters is an operational utility for interacting with the Leonardo Notebook clusters
 * available to the application default user. This should generally be used while authorized as the
 * App Engine default service account for a given environment.
 *
 *
 * Note: If this utility later needs database access, replace @Configuration
 * with @SpringBootApplication.
 */
@Configuration
class ManageClusters {

    @Bean
    fun run(): CommandLineRunner {
        return { args ->
            require(args.size >= 1) { "must specify a command 'list' or 'delete'" }
            val cmd = args[0]
            args = Arrays.copyOfRange<String>(args, 1, args.size)
            when (cmd) {
                "list" -> {
                    require(args.size <= 1) { "Expected 1 arg. Got " + Arrays.asList<String>(*args) }
                    listClusters(args[0])
                    return
                }

                "delete" -> {
                    // User-friendly command-line parsing is done in devstart.rb, so we do only simple
                    // positional argument parsing here.
                    require(args.size == 4) { "Expected 4 args (api_url, min_age, ids, dry_run). Got " + Arrays.asList<String>(*args) }
                    val apiUrl = args[0]
                    var oldest: Instant? = null
                    if (!args[1].isEmpty()) {
                        val age = Duration.ofDays(java.lang.Long.parseLong(args[1]))
                        oldest = Clock.systemUTC().instant().minus(age)
                        log.info("only clusters created before $oldest will be considered")
                    }

                    // Note: IDs are optional, this set may be empty.
                    val ids = commaDelimitedStringToSet(args[2])
                    val dryRun = java.lang.Boolean.valueOf(args[3])
                    deleteClusters(apiUrl, oldest, ids, dryRun)
                    return
                }

                else -> throw IllegalArgumentException(
                        String.format("unrecognized command '%s', want 'list' or 'delete'", cmd))
            }
        }
    }

    companion object {

        private val log = Logger.getLogger(ManageClusters::class.java.name)
        private val BILLING_SCOPES = arrayOf("https://www.googleapis.com/auth/userinfo.profile", "https://www.googleapis.com/auth/userinfo.email")

        private fun commaDelimitedStringToSet(str: String): Set<String> {
            return Arrays.asList(*str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).stream()
                    .filter { s -> !s.isEmpty() }
                    .collect<Set<String>, Any>(Collectors.toSet())
        }

        @Throws(IOException::class)
        private fun newApiClient(apiUrl: String): ClusterApi {
            val apiClient = ApiClient()
            apiClient.setBasePath(apiUrl)
            val credential = GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(*BILLING_SCOPES))
            credential.refreshToken()
            apiClient.setAccessToken(credential.accessToken)
            val api = ClusterApi()
            api.setApiClient(apiClient)
            return api
        }

        private fun clusterId(c: Cluster): String {
            return c.getGoogleProject() + "/" + c.getClusterName()
        }

        private fun formatTabular(c: Cluster): String {
            val gson = Gson()
            val labels = gson.toJsonTree(c.getLabels()).asJsonObject
            var creator = "unknown"
            if (labels.has("created-by")) {
                creator = labels.get("created-by").asString
            }
            var status = ClusterStatus.UNKNOWN
            if (c.getStatus() != null) {
                status = c.getStatus()
            }
            return String.format(
                    "%-40.40s %-50.50s %-10s %-15s", clusterId(c), creator, status, c.getCreatedDate())
        }

        @Throws(IOException::class, ApiException::class)
        private fun listClusters(apiUrl: String) {
            val count = AtomicInteger()
            newApiClient(apiUrl).listClusters(null, false).stream()
                    .sorted(Comparator.comparing<T, U> { c -> Instant.parse(c.getCreatedDate()) })
                    .forEachOrdered(
                            { c ->
                                println(formatTabular(c))
                                count.getAndIncrement()
                            })
            println(String.format("listed %d clusters", count.get()))
        }

        @Throws(IOException::class, ApiException::class)
        private fun deleteClusters(
                apiUrl: String, oldest: Instant?, ids: Set<String>, dryRun: Boolean) {
            val remaining = HashSet(ids)
            val dryMsg = if (dryRun) "[DRY RUN]: would have... " else ""

            val deleted = AtomicInteger()
            val api = newApiClient(apiUrl)
            api.listClusters(null, false).stream()
                    .sorted(Comparator.comparing<T, U> { c -> Instant.parse(c.getCreatedDate()) })
                    .filter(
                            { c ->
                                val createdDate = Instant.parse(c.getCreatedDate())
                                if (oldest != null && createdDate.isAfter(oldest)) {
                                    return@api.listClusters(null, false).stream()
                                            .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
                                    .filter false
                                }
                                if (!ids.isEmpty() && !ids.contains(clusterId(c))) {
                                    return@api.listClusters(null, false).stream()
                                            .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
                                    .filter false
                                }
                                true
                            })
                    .forEachOrdered(
                            { c ->
                                val cid = clusterId(c)
                                if (!dryRun) {
                                    try {
                                        api.deleteCluster(c.getGoogleProject(), c.getClusterName())
                                    } catch (e: ApiException) {
                                        log.log(Level.SEVERE, "failed to deleted cluster $cid", e)
                                        return@api.listClusters(null, false).stream()
                                                .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
                                        .filter(
                                                c -> {
                                            Instant createdDate = Instant . parse c.getCreatedDate();
                                            if (oldest != null && createdDate.isAfter(oldest)) {
                                                return false;
                                            }
                                            return if (!ids.isEmpty() && !ids.contains(clusterId(c))) {
                                                false;
                                            } else true
                                        })
                                        .forEachOrdered
                                    }

                                }
                                remaining.remove(cid)
                                deleted.getAndIncrement()
                                println(dryMsg + "deleted cluster: " + formatTabular(c))
                            })
            if (!remaining.isEmpty()) {
                log.log(
                        Level.SEVERE,
                        "failed to find/delete clusters: {1}",
                        arrayOf<Any>(Joiner.on(", ").join(remaining)))
            }
            println(String.format("%sdeleted %d clusters", dryMsg, deleted.get()))
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(ManageClusters::class.java).web(false).run(*args)
        }
    }
}
