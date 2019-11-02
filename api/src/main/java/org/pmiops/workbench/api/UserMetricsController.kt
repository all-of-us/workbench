package org.pmiops.workbench.api

import com.google.cloud.storage.BlobId
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner
import java.sql.Timestamp
import java.time.Clock
import java.util.Arrays
import java.util.Objects
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentResource
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.model.Cohort
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.FileDetail
import org.pmiops.workbench.model.RecentResource
import org.pmiops.workbench.model.RecentResourceRequest
import org.pmiops.workbench.model.RecentResourceResponse
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserMetricsController @Autowired
internal constructor(
        private val userProvider: Provider<User>,
        private val userRecentResourceService: UserRecentResourceService,
        private val workspaceService: WorkspaceService,
        private val fireCloudService: FireCloudService,
        private val cloudStorageService: CloudStorageService,
        private val clock: Clock) : UserMetricsApiDelegate {
    private var distinctWorkspacelimit = 5

    // Converts DB model to client Model
    private val TO_CLIENT = { userRecentResource ->
        val resource = RecentResource()
        resource.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.cohort))
        resource.setConceptSet(TO_CLIENT_CONCEPT_SET.apply(userRecentResource.conceptSet))
        val fileDetail = convertStringToFileDetail(userRecentResource.notebookName)
        resource.setNotebook(fileDetail)
        resource.setModifiedTime(userRecentResource.lastAccessDate!!.toString())
        resource.setWorkspaceId(userRecentResource.getWorkspaceId())
        resource
    }

    /** Gets the list of all resources recently access by user in order of access date time  */
    // RW-1298
    // This needs to be refactored to only use namespace and FC ID
    // The purpose of this Map, is to check what is actually still present in FC
    // Drop any invalid notebook resources - parseBlobId will log errors.
    // Check for existence of recent notebooks. Notebooks reside in GCS so they may be arbitrarily
    // deleted or renamed without notification to the Workbench. This makes a batch of GCS requests
    // so it will scale fairly well, but limit to the first N notebooks to avoid excess GCS traffic.
    // TODO: If we find a non-existent notebook, expunge from the cache.
    val userRecentResources: ResponseEntity<RecentResourceResponse>
        get() {
            val userId = userProvider.get().userId
            val userRecentResourceList = userRecentResourceService.findAllResourcesByUser(userId)
            val workspaceIdList = userRecentResourceList.stream()
                    .map { it.workspaceId }
                    .distinct()
                    .limit(distinctWorkspacelimit.toLong())
                    .collect<List<Long>, Any>(Collectors.toList())
            val workspaceAccessMap = workspaceIdList.stream()
                    .collect<Map<Long, WorkspaceResponse>, Any>(
                            Collectors.toMap<Any, Any, Any>(
                                    { id -> id },
                                    { id ->
                                        val workspace = workspaceService.findByWorkspaceId(id.toLong())
                                        val workspaceResponse = fireCloudService.getWorkspace(
                                                workspace.workspaceNamespace, workspace.firecloudName)
                                        workspaceResponse
                                    }))

            val workspaceFilteredResources = userRecentResourceList.stream()
                    .filter { r -> workspaceAccessMap.containsKey(r.workspaceId) }
                    .filter { r -> r.notebookName == null || parseBlobId(r.notebookName) != null }
                    .collect<List<UserRecentResource>, Any>(Collectors.toList())
            val foundNotebooks = cloudStorageService.blobsExist(
                    workspaceFilteredResources.stream()
                            .map<BlobId> { r -> parseBlobId(r.notebookName) }
                            .filter(Predicate<BlobId> { Objects.nonNull(it) })
                            .limit(MAX_RECENT_NOTEBOOKS.toLong())
                            .collect<List<BlobId>, Any>(Collectors.toList()))

            val recentResponse = RecentResourceResponse()
            recentResponse.addAll(
                    workspaceFilteredResources.stream()
                            .filter { r -> r.notebookName == null || foundNotebooks.contains(parseBlobId(r.notebookName)) }
                            .map<Any> { userRecentResource ->
                                val resource = TO_CLIENT.apply(userRecentResource)
                                val workspaceDetails = workspaceAccessMap[userRecentResource.workspaceId]
                                resource.setPermission(workspaceDetails.getAccessLevel())
                                resource.setWorkspaceNamespace(workspaceDetails.getWorkspace().getNamespace())
                                resource.setWorkspaceFirecloudName(workspaceDetails.getWorkspace().getName())
                                resource
                            }
                            .collect<R, A>(Collectors.toList<T>()))
            return ResponseEntity.ok<RecentResourceResponse>(recentResponse)
        }

    @VisibleForTesting
    fun setDistinctWorkspaceLimit(limit: Int) {
        distinctWorkspacelimit = limit
    }

    fun updateRecentResource(
            workspaceNamespace: String, workspaceId: String, recentResourceRequest: RecentResourceRequest): ResponseEntity<RecentResource> {
        // this is only ever used for Notebooks because we update/add to the cache for the other
        // resources in the backend
        // Because we don't store notebooks in our database the way we do other resources.
        val now = Timestamp(clock.instant().toEpochMilli())
        val wId = getWorkspaceId(workspaceNamespace, workspaceId)
        val notebookPath: String
        if (recentResourceRequest.getNotebookName().startsWith("gs://")) {
            notebookPath = recentResourceRequest.getNotebookName()
        } else {
            val bucket = fireCloudService
                    .getWorkspace(workspaceNamespace, workspaceId)
                    .getWorkspace()
                    .getBucketName()
            notebookPath = "gs://" + bucket + "/notebooks/" + recentResourceRequest.getNotebookName()
        }
        val recentResource = userRecentResourceService.updateNotebookEntry(
                wId, userProvider.get().userId, notebookPath, now)
        return ResponseEntity.ok<RecentResource>(TO_CLIENT.apply(recentResource))
    }

    fun deleteRecentResource(
            workspaceNamespace: String, workspaceId: String, recentResourceRequest: RecentResourceRequest): ResponseEntity<EmptyResponse> {
        val wId = getWorkspaceId(workspaceNamespace, workspaceId)
        userRecentResourceService.deleteNotebookEntry(
                wId, userProvider.get().userId, recentResourceRequest.getNotebookName())
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    // Retrieves Database workspace ID
    private fun getWorkspaceId(workspaceNamespace: String, workspaceId: String): Long {
        val dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId)
        return dbWorkspace.workspaceId
    }

    private fun parseBlobId(uri: String?): BlobId? {
        var uri: String? = uri ?: return null
        if (!uri!!.startsWith("gs://")) {
            log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", uri))
            return null
        }
        uri = uri.replaceFirst("gs://".toRegex(), "")
        val parts = uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size <= 1) {
            log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", uri))
            return null
        }
        return BlobId.of(parts[0], Joiner.on('/').join(Arrays.copyOfRange(parts, 1, parts.size)))
    }

    private fun convertStringToFileDetail(str: String?): FileDetail? {
        if (str == null) {
            return null
        }
        if (!str.startsWith("gs://")) {
            log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", str))
            return null
        }
        val pos = str.lastIndexOf('/') + 1
        val fileName = str.substring(pos)
        val replacement = Matcher.quoteReplacement(fileName) + "$"
        val filePath = str.replaceFirst(replacement.toRegex(), "")
        return FileDetail().name(fileName).path(filePath)
    }

    companion object {
        private val MAX_RECENT_NOTEBOOKS = 8
        private val log = Logger.getLogger(UserMetricsController::class.java.name)

        private val TO_CLIENT_COHORT = object : Function<org.pmiops.workbench.db.model.Cohort, Cohort> {
            override fun apply(cohort: org.pmiops.workbench.db.model.Cohort?): Cohort? {
                if (cohort == null) {
                    return null
                }
                val result = Cohort()
                        .etag(Etags.fromVersion(cohort.version))
                        .lastModifiedTime(cohort.lastModifiedTime!!.time)
                        .creationTime(cohort.creationTime!!.time)
                        .criteria(cohort.criteria)
                        .description(cohort.description)
                        .id(cohort.cohortId)
                        .name(cohort.name)
                        .type(cohort.type)
                if (cohort.creator != null) {
                    result.setCreator(cohort.creator!!.email)
                }
                return result
            }
        }

        private val TO_CLIENT_CONCEPT_SET = object : Function<org.pmiops.workbench.db.model.ConceptSet, ConceptSet> {
            override fun apply(conceptSet: org.pmiops.workbench.db.model.ConceptSet?): ConceptSet? {
                return if (conceptSet == null) {
                    null
                } else ConceptSet()
                        .etag(Etags.fromVersion(conceptSet.version))
                        .lastModifiedTime(conceptSet.lastModifiedTime!!.time)
                        .creationTime(conceptSet.creationTime!!.time)
                        .description(conceptSet.description)
                        .id(conceptSet.conceptSetId)
                        .name(conceptSet.name)
            }
        }
    }
}
