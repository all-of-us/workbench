package org.pmiops.workbench.tools

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.pmiops.workbench.firecloud.ApiClient
import org.pmiops.workbench.firecloud.ApiException
import org.pmiops.workbench.firecloud.api.BillingApi
import org.pmiops.workbench.firecloud.api.WorkspacesApi
import org.pmiops.workbench.firecloud.model.Workspace
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Backfill script for granting the billing project user role to all AoU owners (corresponding to
 * Firecloud OWNER access). For http://broad.io/1ppw, collaborators need to be either writers or
 * billing project users in order to launch clusters within shared billing projects, see RW-3009 and
 * RW-3188 for details.
 */
@Configuration
class BackfillBillingProjectUsers {

    @Bean
    fun run(): CommandLineRunner {
        return { args ->
            val opts = DefaultParser().parse(options, args)
            backfill(
                    newWorkspacesApi(opts.getOptionValue(fcBaseUrlOpt.longOpt)),
                    newBillingApi(opts.getOptionValue(fcBaseUrlOpt.longOpt)),
                    opts.getOptionValue(billingProjectPrefixOpt.longOpt),
                    opts.hasOption(dryRunOpt.longOpt))
        }
    }

    companion object {
        val FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS: List<String> = ImmutableList.of(
                "accessLevel", "workspace.namespace", "workspace.name", "workspace.createdBy")

        private val fcBaseUrlOpt = Option.builder()
                .longOpt("fc-base-url")
                .desc("Firecloud API base URL")
                .required()
                .hasArg()
                .build()
        private val billingProjectPrefixOpt = Option.builder()
                .longOpt("billing-project-prefix")
                .desc("Billing project prefix to filter by, other workspaces are ignored")
                .required()
                .hasArg()
                .build()
        private val dryRunOpt = Option.builder()
                .longOpt("dry-run")
                .desc("If specified, the tool runs in dry run mode; no modifications are made")
                .build()
        private val options = Options().addOption(fcBaseUrlOpt).addOption(billingProjectPrefixOpt).addOption(dryRunOpt)

        private val FC_SCOPES = arrayOf("https://www.googleapis.com/auth/userinfo.profile", "https://www.googleapis.com/auth/userinfo.email")

        private val log = Logger.getLogger(BackfillBillingProjectUsers::class.java.name)

        @Throws(IOException::class)
        private fun newApiClient(apiUrl: String): ApiClient {
            val apiClient = ApiClient()
            apiClient.setBasePath(apiUrl)
            val credential = GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(*FC_SCOPES))
            credential.refreshToken()
            apiClient.setAccessToken(credential.accessToken)
            return apiClient
        }

        @Throws(IOException::class)
        private fun newWorkspacesApi(apiUrl: String): WorkspacesApi {
            val api = WorkspacesApi()
            api.setApiClient(newApiClient(apiUrl))
            return api
        }

        @Throws(IOException::class)
        private fun newBillingApi(apiUrl: String): BillingApi {
            val api = BillingApi()
            api.setApiClient(newApiClient(apiUrl))
            return api
        }

        private fun dryLog(dryRun: Boolean, msg: String) {
            var prefix = ""
            if (dryRun) {
                prefix = "[DRY RUN] Would have... "
            }
            log.info(prefix + msg)
        }

        /**
         * Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
         * instead. Run this through a typed Gson conversion process to coerce it into the desired type.
         */
        private fun extractAclResponse(aclResp: WorkspaceACL): Map<String, WorkspaceAccessEntry> {
            val accessEntryType = object : TypeToken<Map<String, WorkspaceAccessEntry>>() {

            }.type
            val gson = Gson()
            return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType)
        }

        @Throws(ApiException::class)
        private fun backfill(
                workspacesApi: WorkspacesApi,
                billingApi: BillingApi,
                billingProjectPrefix: String,
                dryRun: Boolean) {
            var userUpgrades = 0
            for (resp in workspacesApi.listWorkspaces(FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS)) {
                val w = resp.getWorkspace()
                if (!w.getNamespace().startsWith(billingProjectPrefix)) {
                    continue
                }

                val id = w.getNamespace() + "/" + w.getName()
                if ("PROJECT_OWNER" != resp.getAccessLevel()) {
                    log.warning(
                            String.format(
                                    "service account has '%s' access to workspace '%s'; skipping",
                                    resp.getAccessLevel(), id))
                    continue
                }

                val acl = extractAclResponse(workspacesApi.getWorkspaceAcl(w.getNamespace(), w.getName()))
                for (user in acl.keys) {
                    if (user == w.getCreatedBy()) {
                        // Skip the common case, creators should already be billing project users. Any edge cases
                        // where this is not true will be fixed by the 1PPW migration (RW-2705).
                        continue
                    }
                    val entry = acl[user]
                    if ("OWNER" != entry.getAccessLevel()) {
                        // Only owners should be granted billing project user.
                        continue
                    }
                    dryLog(
                            dryRun,
                            String.format(
                                    "granting billing project user on '%s' to '%s' (%s)",
                                    w.getNamespace(), user, entry.getAccessLevel()))
                    if (!dryRun) {
                        try {
                            billingApi.addUserToBillingProject(w.getNamespace(), "user", user)
                        } catch (e: ApiException) {
                            log.log(Level.WARNING, "failed to add user to project", e)
                        }

                    }
                    userUpgrades++
                }
            }

            dryLog(dryRun, String.format("added %d users as billing project users", userUpgrades))
        }

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(BackfillBillingProjectUsers::class.java).web(false).run(*args)
        }
    }
}
