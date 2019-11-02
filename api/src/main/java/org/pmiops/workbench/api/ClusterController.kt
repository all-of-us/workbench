package org.pmiops.workbench.api

import com.google.gson.Gson
import java.net.MalformedURLException
import java.net.URL
import java.sql.Timestamp
import java.time.Clock
import java.util.Base64
import java.util.HashMap
import java.util.Optional
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.json.JSONObject
import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.User.ClusterConfig
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.Cluster
import org.pmiops.workbench.model.ClusterListResponse
import org.pmiops.workbench.model.ClusterLocalizeRequest
import org.pmiops.workbench.model.ClusterLocalizeResponse
import org.pmiops.workbench.model.ClusterStatus
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.UpdateClusterConfigRequest
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.pmiops.workbench.notebooks.model.ClusterError
import org.pmiops.workbench.notebooks.model.StorageLink
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ClusterController @Autowired
internal constructor(
        private val leonardoNotebooksClient: LeonardoNotebooksClient,
        private val userProvider: Provider<User>,
        private val workspaceService: WorkspaceService,
        private val fireCloudService: FireCloudService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>,
        private val userService: UserService,
        private val userRecentResourceService: UserRecentResourceService,
        private val userDao: UserDao,
        private val clock: Clock) : ClusterApiDelegate {

    fun listClusters(
            billingProjectId: String?, workspaceName: String): ResponseEntity<ClusterListResponse> {
        if (billingProjectId == null) {
            throw BadRequestException("Must specify billing project")
        }

        val user = this.userProvider.get()

        val clusterName = clusterNameForUser(user)

        var fcCluster: org.pmiops.workbench.notebooks.model.Cluster
        try {
            fcCluster = this.leonardoNotebooksClient.getCluster(billingProjectId, clusterName)
        } catch (e: NotFoundException) {
            fcCluster = this.leonardoNotebooksClient.createCluster(billingProjectId, clusterName, workspaceName)
        }

        val retries = Optional.ofNullable(user.clusterCreateRetries).orElse(0)
        if (org.pmiops.workbench.notebooks.model.ClusterStatus.ERROR.equals(fcCluster.getStatus())) {
            if (retries <= 2) {
                this.userService.setClusterRetryCount(retries + 1)
                log.warning("Cluster has errored with logs: ")
                if (fcCluster.getErrors() != null) {
                    for (e in fcCluster.getErrors()) {
                        log.warning(e.getErrorMessage())
                    }
                }
                log.warning("Retrying cluster creation.")

                this.leonardoNotebooksClient.deleteCluster(billingProjectId, clusterName)
            }
        } else if (org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING.equals(
                        fcCluster.getStatus()) && retries != 0) {
            this.userService.setClusterRetryCount(0)
        }
        val resp = ClusterListResponse()
        resp.setDefaultCluster(TO_ALL_OF_US_CLUSTER.apply(fcCluster))
        return ResponseEntity.ok<ClusterListResponse>(resp)
    }

    fun deleteCluster(projectName: String, clusterName: String): ResponseEntity<EmptyResponse> {
        this.userService.setClusterRetryCount(0)
        this.leonardoNotebooksClient.deleteCluster(projectName, clusterName)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun localize(
            projectName: String, clusterName: String, body: ClusterLocalizeRequest): ResponseEntity<ClusterLocalizeResponse> {
        val fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace
        try {
            fcWorkspace = fireCloudService
                    .getWorkspace(body.getWorkspaceNamespace(), body.getWorkspaceId())
                    .getWorkspace()
        } catch (e: NotFoundException) {
            throw NotFoundException(
                    String.format(
                            "workspace %s/%s not found or not accessible",
                            body.getWorkspaceNamespace(), body.getWorkspaceId()))
        }

        val cdrVersion = workspaceService
                .getRequired(body.getWorkspaceNamespace(), body.getWorkspaceId())
                .cdrVersion

        // For the common case where the notebook cluster matches the workspace
        // namespace, simply name the directory as the workspace ID; else we
        // include the namespace in the directory name to avoid possible conflicts
        // in workspace IDs.
        val gcsNotebooksDir = "gs://" + fcWorkspace.getBucketName() + "/notebooks"
        val now = Timestamp(clock.instant().toEpochMilli())
        val workspaceId = workspaceService
                .getRequired(body.getWorkspaceNamespace(), body.getWorkspaceId())
                .workspaceId

        body.getNotebookNames()
                .forEach { notebook ->
                    userRecentResourceService.updateNotebookEntry(
                            workspaceId,
                            userProvider.get().userId,
                            "$gcsNotebooksDir/$notebook",
                            now)
                }
        var workspacePath = body.getWorkspaceId()
        if (projectName != body.getWorkspaceNamespace()) {
            workspacePath = (body.getWorkspaceNamespace()
                    + FireCloudService.WORKSPACE_DELIMITER
                    + body.getWorkspaceId())
        }

        val editDir = "workspaces/$workspacePath"
        val playgroundDir = "workspaces_playground/$workspacePath"
        val targetDir = if (body.getPlaygroundMode()) playgroundDir else editDir

        leonardoNotebooksClient.createStorageLink(
                projectName,
                clusterName,
                StorageLink()
                        .cloudStorageDirectory(gcsNotebooksDir)
                        .localBaseDirectory(editDir)
                        .localSafeModeBaseDirectory(playgroundDir)
                        .pattern(DELOC_PATTERN))

        // Always localize config files; usually a no-op after the first call.
        val localizeMap = HashMap<String, String>()

        // The Welder extension offers direct links to/from playground mode; write the AoU config file
        // to both locations so notebooks will work in either directory.
        val aouConfigUri = aouConfigDataUri(fcWorkspace, cdrVersion, projectName)
        localizeMap["$editDir/$AOU_CONFIG_FILENAME"] = aouConfigUri
        localizeMap["$playgroundDir/$AOU_CONFIG_FILENAME"] = aouConfigUri

        // Localize the requested notebooks, if any.
        if (body.getNotebookNames() != null) {
            localizeMap.putAll(
                    body.getNotebookNames().stream()
                            .collect(
                                    Collectors.toMap<T, K, U>(
                                            { name -> "$targetDir/$name" }, { name -> "$gcsNotebooksDir/$name" })))
        }

        leonardoNotebooksClient.localize(projectName, clusterName, localizeMap)

        // This is the Jupyer-server-root-relative path, the style used by the Jupyter REST API.
        return ResponseEntity.ok(ClusterLocalizeResponse().clusterLocalDirectory(targetDir))
    }

    @AuthorityRequired(Authority.DEVELOPER)
    fun updateClusterConfig(body: UpdateClusterConfigRequest): ResponseEntity<EmptyResponse> {
        val user = userDao.findUserByEmail(body.getUserEmail())
                ?: throw NotFoundException("User '" + body.getUserEmail() + "' not found")
        val oldOverride = user.clusterConfigDefaultRaw

        val override = if (body.getClusterConfig() != null) ClusterConfig() else null
        if (override != null) {
            override.masterDiskSize = body.getClusterConfig().getMasterDiskSize()
            override.machineType = body.getClusterConfig().getMachineType()
        }
        userService.updateUserWithRetries(
                { u ->
                    u.clusterConfigDefault = override
                    u
                },
                user)
        userService.logAdminUserAction(
                user.userId, "cluster config override", oldOverride, Gson().toJson(override))
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    private fun jsonToDataUri(json: JSONObject): String {
        return DATA_URI_PREFIX + Base64.getUrlEncoder().encodeToString(json.toString().toByteArray())
    }

    private fun aouConfigDataUri(
            fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace,
            cdrVersion: CdrVersion?,
            cdrBillingCloudProject: String): String {
        val config = JSONObject()

        var host: String? = null
        try {
            host = URL(workbenchConfigProvider.get().server.apiBaseUrl).host
        } catch (e: MalformedURLException) {
            log.log(Level.SEVERE, "bad apiBaseUrl config value; failing", e)
            throw ServerErrorException("Failed to generate AoU notebook config")
        }

        config.put(WORKSPACE_NAMESPACE_KEY, fcWorkspace.getNamespace())
        config.put(WORKSPACE_ID_KEY, fcWorkspace.getName())
        config.put(BUCKET_NAME_KEY, fcWorkspace.getBucketName())
        config.put(API_HOST_KEY, host)
        config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersion!!.bigqueryProject)
        config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersion.bigqueryDataset)
        config.put(BILLING_CLOUD_PROJECT, cdrBillingCloudProject)
        return jsonToDataUri(config)
    }

    companion object {

        // This file is used by the All of Us libraries to access workspace/CDR metadata.
        private val AOU_CONFIG_FILENAME = ".all_of_us_config.json"
        private val WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE"
        private val WORKSPACE_ID_KEY = "WORKSPACE_ID"
        private val API_HOST_KEY = "API_HOST"
        private val BUCKET_NAME_KEY = "BUCKET_NAME"
        private val CDR_VERSION_CLOUD_PROJECT = "CDR_VERSION_CLOUD_PROJECT"
        private val CDR_VERSION_BIGQUERY_DATASET = "CDR_VERSION_BIGQUERY_DATASET"
        // The billing project to use for the analysis.
        private val BILLING_CLOUD_PROJECT = "BILLING_CLOUD_PROJECT"
        private val DATA_URI_PREFIX = "data:application/json;base64,"
        private val DELOC_PATTERN = "\\.ipynb$"

        private val log = Logger.getLogger(ClusterController::class.java.name)

        private val TO_ALL_OF_US_CLUSTER = { firecloudCluster ->
            val allOfUsCluster = Cluster()
            allOfUsCluster.setClusterName(firecloudCluster.getClusterName())
            allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject())
            var status = ClusterStatus.UNKNOWN
            if (firecloudCluster.getStatus() != null) {
                val converted = ClusterStatus.fromValue(firecloudCluster.getStatus().toString())
                if (converted != null) {
                    status = converted
                } else {
                    log.warning("unknown Leonardo status: " + firecloudCluster.getStatus())
                }
            }
            allOfUsCluster.setStatus(status)
            allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate())
            allOfUsCluster
        }

        private fun clusterNameForUser(user: User): String {
            return "all-of-us-" + user.userId
        }
    }
}
