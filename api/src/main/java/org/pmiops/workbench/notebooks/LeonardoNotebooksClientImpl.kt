package org.pmiops.workbench.notebooks

import com.google.common.collect.ImmutableMap
import java.util.HashMap
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.User.ClusterConfig
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.api.NotebooksApi
import org.pmiops.workbench.notebooks.api.StatusApi
import org.pmiops.workbench.notebooks.model.Cluster
import org.pmiops.workbench.notebooks.model.ClusterRequest
import org.pmiops.workbench.notebooks.model.LocalizationEntry
import org.pmiops.workbench.notebooks.model.Localize
import org.pmiops.workbench.notebooks.model.MachineConfig
import org.pmiops.workbench.notebooks.model.StorageLink
import org.pmiops.workbench.notebooks.model.UserJupyterExtensionConfig
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class LeonardoNotebooksClientImpl @Autowired
constructor(
        @param:Qualifier(NotebooksConfig.USER_CLUSTER_API) private val clusterApiProvider: Provider<ClusterApi>,
        private val notebooksApiProvider: Provider<NotebooksApi>,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>,
        private val userProvider: Provider<User>,
        private val retryHandler: NotebooksRetryHandler,
        private val workspaceService: WorkspaceService) : LeonardoNotebooksClient {

    override// If any of the systems for notebooks are down, it won't work for us.
    val notebooksStatus: Boolean
        get() {
            try {
                StatusApi().getSystemStatus()
            } catch (e: ApiException) {
                log.log(Level.WARNING, "notebooks status check request failed", e)
                return false
            }

            return true
        }

    private fun createFirecloudClusterRequest(
            userEmail: String?,
            workspaceNamespace: String,
            workspaceName: String,
            clusterOverride: ClusterConfig?): ClusterRequest {
        var clusterOverride = clusterOverride
        if (clusterOverride == null) {
            clusterOverride = ClusterConfig()
        }

        val config = workbenchConfigProvider.get()
        val gcsPrefix = "gs://" + config.googleCloudStorageService.clusterResourcesBucketName

        val nbExtensions = HashMap<String, String>()
        nbExtensions["aou-snippets-menu"] = "$gcsPrefix/aou-snippets-menu.js"
        nbExtensions["aou-download-extension"] = "$gcsPrefix/aou-download-policy-extension.js"
        nbExtensions["aou-activity-checker-extension"] = "$gcsPrefix/activity-checker-extension.js"

        val workspace = workspaceService.getRequired(workspaceNamespace, workspaceName)
        val customClusterEnvironmentVariables = HashMap<String, String>()
        // i.e. is NEW or MIGRATED
        if (workspace.billingMigrationStatusEnum != BillingMigrationStatus.OLD) {
            customClusterEnvironmentVariables[WORKSPACE_CDR] = (workspace.cdrVersion!!.bigqueryProject
                    + "."
                    + workspace.cdrVersion!!.bigqueryDataset)
        }

        return ClusterRequest()
                .labels(ImmutableMap.of<K, V>(CLUSTER_LABEL_AOU, "true", CLUSTER_LABEL_CREATED_BY, userEmail!!))
                .defaultClientId(config.server.oauthClientId)
                // Note: Filenames must be kept in sync with files in cluster-resources directory.
                .jupyterUserScriptUri("$gcsPrefix/setup_notebook_cluster.sh")
                .userJupyterExtensionConfig(
                        UserJupyterExtensionConfig()
                                .nbExtensions(nbExtensions)
                                .serverExtensions(ImmutableMap.of<K, V>("jupyterlab", "jupyterlab"))
                                .combinedExtensions(ImmutableMap.of<String, String>())
                                .labExtensions(ImmutableMap.of<String, String>()))
                // Matches Terra UI's scopes, see RW-3531 for rationale.
                .addScopesItem("https://www.googleapis.com/auth/cloud-platform")
                .addScopesItem("https://www.googleapis.com/auth/userinfo.email")
                .addScopesItem("https://www.googleapis.com/auth/userinfo.profile")
                .enableWelder(true)
                .machineConfig(
                        MachineConfig()
                                .masterDiskSize(
                                        Optional.ofNullable(clusterOverride.masterDiskSize)
                                                .orElse(config.firecloud.clusterDefaultDiskSizeGb))
                                .masterMachineType(
                                        Optional.ofNullable(clusterOverride.machineType)
                                                .orElse(config.firecloud.clusterDefaultMachineType)))
                .customClusterEnvironmentVariables(customClusterEnvironmentVariables)
    }

    override fun createCluster(googleProject: String, clusterName: String, workspaceName: String): Cluster {
        val clusterApi = clusterApiProvider.get()
        val user = userProvider.get()
        return retryHandler.run<Any> { context ->
            clusterApi.createClusterV2(
                    googleProject,
                    clusterName,
                    createFirecloudClusterRequest(
                            user.email,
                            googleProject,
                            workspaceName,
                            user.clusterConfigDefault))
        }
    }

    override fun deleteCluster(googleProject: String, clusterName: String) {
        val clusterApi = clusterApiProvider.get()
        retryHandler.run<Any> { context ->
            clusterApi.deleteCluster(googleProject, clusterName)
            null
        }
    }

    override fun listClusters(labels: String, includeDeleted: Boolean): List<Cluster> {
        val clusterApi = clusterApiProvider.get()
        return retryHandler.run<List<Cluster>> { context -> clusterApi.listClusters(labels, includeDeleted) }
    }

    override fun getCluster(googleProject: String, clusterName: String): Cluster {
        val clusterApi = clusterApiProvider.get()
        return retryHandler.run<Any> { context -> clusterApi.getCluster(googleProject, clusterName) }
    }

    override fun localize(googleProject: String, clusterName: String, fileList: Map<String, String>) {
        val welderReq = Localize()
                .entries(
                        fileList.entries.stream()
                                .map<Any> { e ->
                                    LocalizationEntry()
                                            .sourceUri(e.value)
                                            .localDestinationPath(e.key)
                                }
                                .collect<R, A>(Collectors.toList<T>()))
        val notebooksApi = notebooksApiProvider.get()
        retryHandler.run<Any> { context ->
            notebooksApi.welderLocalize(googleProject, clusterName, welderReq)
            null
        }
    }

    override fun createStorageLink(
            googleProject: String, clusterName: String, storageLink: StorageLink): StorageLink {
        val notebooksApi = notebooksApiProvider.get()
        return retryHandler.run<Any> { context -> notebooksApi.welderCreateStorageLink(googleProject, clusterName, storageLink) }
    }

    companion object {

        private val CLUSTER_LABEL_AOU = "all-of-us"
        private val CLUSTER_LABEL_CREATED_BY = "created-by"
        private val WORKSPACE_CDR = "WORKSPACE_CDR"

        private val log = Logger.getLogger(LeonardoNotebooksClientImpl::class.java.name)
    }
}
