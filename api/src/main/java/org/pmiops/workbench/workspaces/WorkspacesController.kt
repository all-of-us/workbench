package org.pmiops.workbench.workspaces

import com.github.rholder.retry.RetryException
import com.github.rholder.retry.Retryer
import com.github.rholder.retry.RetryerBuilder
import com.github.rholder.retry.StopStrategies
import com.github.rholder.retry.WaitStrategies
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageException
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.collect.ImmutableMap
import com.google.common.io.BaseEncoding
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.sql.Timestamp
import java.time.Clock
import java.util.HashMap
import java.util.Optional
import java.util.Random
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.api.Etags
import org.pmiops.workbench.api.WorkspacesApiDelegate
import org.pmiops.workbench.audit.adapters.WorkspaceAuditAdapterService
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.billing.EmptyBufferException
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.FailedPreconditionException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.TooManyRequestsException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.model.ArchivalStatus
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.CloneWorkspaceRequest
import org.pmiops.workbench.model.CloneWorkspaceResponse
import org.pmiops.workbench.model.CopyRequest
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.FileDetail
import org.pmiops.workbench.model.NotebookLockingMetadataResponse
import org.pmiops.workbench.model.NotebookRename
import org.pmiops.workbench.model.ReadOnlyNotebookResponse
import org.pmiops.workbench.model.RecentWorkspace
import org.pmiops.workbench.model.RecentWorkspaceResponse
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.ResearchPurposeReviewRequest
import org.pmiops.workbench.model.ShareWorkspaceRequest
import org.pmiops.workbench.model.UpdateWorkspaceRequest
import org.pmiops.workbench.model.UserRole
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.pmiops.workbench.model.WorkspaceListResponse
import org.pmiops.workbench.model.WorkspaceResponse
import org.pmiops.workbench.model.WorkspaceResponseListResponse
import org.pmiops.workbench.model.WorkspaceUserRolesResponse
import org.pmiops.workbench.notebooks.BlobAlreadyExistsException
import org.pmiops.workbench.notebooks.NotebooksService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class WorkspacesController @Autowired
constructor(
        private val billingProjectBufferService: BillingProjectBufferService,
        private val workspaceService: WorkspaceService,
        private val cdrVersionDao: CdrVersionDao,
        private val userDao: UserDao,
        private var userProvider: Provider<User>?,
        private val fireCloudService: FireCloudService,
        private val cloudStorageService: CloudStorageService,
        private val clock: Clock,
        private val notebooksService: NotebooksService,
        private val userService: UserService,
        private var workbenchConfigProvider: Provider<WorkbenchConfig>?,
        private val workspaceAuditAdapterService: WorkspaceAuditAdapterService) : WorkspacesApiDelegate {

    private val retryer = RetryerBuilder.newBuilder<Boolean>()
            .retryIfExceptionOfType(StorageException::class.java)
            .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(12))
            .build()

    private val registeredUserDomainEmail: String
        get() {
            val registeredDomainGroup = fireCloudService.getGroup(workbenchConfigProvider!!.get().firecloud.registeredDomainName)
            return registeredDomainGroup.getGroupEmail()
        }

    val workspaces: ResponseEntity<WorkspaceResponseListResponse>
        get() {
            val response = WorkspaceResponseListResponse()
            response.setItems(workspaceService.workspaces)
            return ResponseEntity.ok<WorkspaceResponseListResponse>(response)
        }

    // Note we do not paginate the workspaces list, since we expect few workspaces
    // to require review.
    //
    // We can add pagination in the DAO by returning Slice<Workspace> if we want the method to return
    // pagination information (e.g. are there more workspaces to get), and Page<Workspace> if we
    // want the method to return both pagination information and a total count.
    val workspacesForReview: ResponseEntity<WorkspaceListResponse>
        @AuthorityRequired(Authority.REVIEW_RESEARCH_PURPOSE)
        get() {
            val response = WorkspaceListResponse()
            val workspaces = workspaceService.findForReview()
            response.setItems(
                    workspaces.stream()
                            .map<Workspace> { WorkspaceConversionUtils.toApiWorkspace(it) }
                            .collect(Collectors.toList<T>()))
            return ResponseEntity.ok<WorkspaceListResponse>(response)
        }

    val publishedWorkspaces: ResponseEntity<WorkspaceResponseListResponse>
        get() {
            val response = WorkspaceResponseListResponse()
            response.setItems(workspaceService.publishedWorkspaces)
            return ResponseEntity.ok<WorkspaceResponseListResponse>(response)
        }

    val userRecentWorkspaces: ResponseEntity<RecentWorkspaceResponse>
        get() {
            val userRecentWorkspaces = workspaceService.recentWorkspaces
            val workspaceIds = userRecentWorkspaces.stream()
                    .map { it.workspaceId }
                    .collect<List<Long>, Any>(Collectors.toList())
            val dbWorkspaces = workspaceService.dao.findAllByWorkspaceIdIn(workspaceIds)
            val dbWorkspacesById = dbWorkspaces.stream()
                    .collect<Map<Long, org.pmiops.workbench.db.model.Workspace>, Any>(
                            Collectors.toMap<Workspace, Long, Workspace>(
                                    Function<Workspace, Long> { it.getWorkspaceId() }, Function.identity<Workspace>()))
            val workspaceAccessLevelsById = dbWorkspaces.stream()
                    .collect<Map<Long, WorkspaceAccessLevel>, Any>(
                            Collectors.toMap<Workspace, Long, Any>(
                                    Function<Workspace, Long> { it.getWorkspaceId() },
                                    { dbWorkspace ->
                                        workspaceService.getWorkspaceAccessLevel(
                                                dbWorkspace.workspaceNamespace, dbWorkspace.firecloudName)
                                    }))

            val recentWorkspaceResponse = RecentWorkspaceResponse()
            val recentWorkspaces = WorkspaceConversionUtils.buildRecentWorkspaceList(
                    userRecentWorkspaces, dbWorkspacesById, workspaceAccessLevelsById)
            recentWorkspaceResponse.addAll(recentWorkspaces)
            return ResponseEntity.ok<RecentWorkspaceResponse>(recentWorkspaceResponse)
        }

    @VisibleForTesting
    fun setUserProvider(userProvider: Provider<User>) {
        this.userProvider = userProvider
    }

    @VisibleForTesting
    internal fun setWorkbenchConfigProvider(workbenchConfigProvider: Provider<WorkbenchConfig>) {
        this.workbenchConfigProvider = workbenchConfigProvider
    }

    private fun setLiveCdrVersionId(
            dbWorkspace: org.pmiops.workbench.db.model.Workspace, cdrVersionId: String): CdrVersion {
        if (Strings.isNullOrEmpty(cdrVersionId)) {
            throw BadRequestException("missing cdrVersionId")
        }
        try {
            val cdrVersion = cdrVersionDao.findOne(java.lang.Long.parseLong(cdrVersionId))
                    ?: throw BadRequestException(
                            String.format("CDR version with ID %s not found", cdrVersionId))
            if (ArchivalStatus.LIVE !== cdrVersion.archivalStatusEnum) {
                throw FailedPreconditionException(
                        String.format(
                                "CDR version with ID %s is not live, please select a different CDR version",
                                cdrVersionId))
            }
            dbWorkspace.cdrVersion = cdrVersion
            return cdrVersion
        } catch (e: NumberFormatException) {
            throw BadRequestException(String.format("Invalid cdr version ID: %s", cdrVersionId))
        }

    }

    private fun generateFirecloudWorkspaceId(namespace: String?, name: String): FirecloudWorkspaceId {
        // Find a unique workspace namespace based off of the provided name.
        var strippedName = name.toLowerCase().replace("[^0-9a-z]".toRegex(), "")
        // If the stripped name has no chars, generate a random name.
        if (strippedName.isEmpty()) {
            strippedName = generateRandomChars(RANDOM_CHARS, NUM_RANDOM_CHARS)
        }
        return FirecloudWorkspaceId(namespace, strippedName)
    }

    private fun attemptFirecloudWorkspaceCreation(
            workspaceId: FirecloudWorkspaceId): org.pmiops.workbench.firecloud.model.Workspace {
        return fireCloudService.createWorkspace(
                workspaceId.workspaceNamespace, workspaceId.workspaceName)
    }

    fun createWorkspace(workspace: Workspace): ResponseEntity<Workspace> {
        if (Strings.isNullOrEmpty(workspace.getName())) {
            throw BadRequestException("missing required field 'name'")
        } else if (workspace.getResearchPurpose() == null) {
            throw BadRequestException("missing required field 'researchPurpose'")
        } else if (workspace.getDataAccessLevel() == null) {
            throw BadRequestException("missing required field 'dataAccessLevel'")
        } else if (workspace.getName().length() > 80) {
            throw BadRequestException("Workspace name must be 80 characters or less")
        }

        val user = userProvider!!.get()
        val workspaceNamespace: String?
        val bufferedBillingProject: BillingProjectBufferEntry
        try {
            bufferedBillingProject = billingProjectBufferService.assignBillingProject(user)
        } catch (e: EmptyBufferException) {
            throw TooManyRequestsException()
        }

        workspaceNamespace = bufferedBillingProject.fireCloudProjectName

        // Note: please keep any initialization logic here in sync with CloneWorkspace().
        val workspaceId = generateFirecloudWorkspaceId(workspaceNamespace, workspace.getName())
        val fcWorkspace = attemptFirecloudWorkspaceCreation(workspaceId)

        val now = Timestamp(clock.instant().toEpochMilli())
        var dbWorkspace = org.pmiops.workbench.db.model.Workspace()
        setDbWorkspaceFields(dbWorkspace, user, workspaceId, fcWorkspace, now)

        setLiveCdrVersionId(dbWorkspace, workspace.getCdrVersionId())

        val reqWorkspace = WorkspaceConversionUtils.toDbWorkspace(workspace)
        // TODO: enforce data access level authorization
        dbWorkspace.dataAccessLevel = reqWorkspace.dataAccessLevel
        dbWorkspace.name = reqWorkspace.name

        // Ignore incoming fields pertaining to review status; clients can only request a review.
        WorkspaceConversionUtils.setResearchPurposeDetails(dbWorkspace, workspace.getResearchPurpose())
        if (reqWorkspace.reviewRequested!!) {
            // Use a consistent timestamp.
            dbWorkspace.timeRequested = now
        }
        dbWorkspace.reviewRequested = reqWorkspace.reviewRequested

        dbWorkspace.billingMigrationStatusEnum = BillingMigrationStatus.NEW

        dbWorkspace = workspaceService.dao.save(dbWorkspace)
        val createdWorkspace = WorkspaceConversionUtils.toApiWorkspace(dbWorkspace, fcWorkspace)
        workspaceAuditAdapterService.fireCreateAction(createdWorkspace, dbWorkspace.workspaceId)
        return ResponseEntity.ok<Workspace>(createdWorkspace)
    }

    private fun setDbWorkspaceFields(
            dbWorkspace: org.pmiops.workbench.db.model.Workspace,
            user: User,
            workspaceId: FirecloudWorkspaceId,
            fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace,
            createdAndLastModifiedTime: Timestamp) {
        dbWorkspace.firecloudName = workspaceId.workspaceName
        dbWorkspace.workspaceNamespace = workspaceId.workspaceNamespace
        dbWorkspace.creator = user
        dbWorkspace.firecloudUuid = fcWorkspace.getWorkspaceId()
        dbWorkspace.creationTime = createdAndLastModifiedTime
        dbWorkspace.lastModifiedTime = createdAndLastModifiedTime
        dbWorkspace.version = 1
        dbWorkspace.workspaceActiveStatusEnum = WorkspaceActiveStatus.ACTIVE
    }

    fun deleteWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<EmptyResponse> {
        // This deletes all Firecloud and google resources, however saves all references
        // to the workspace and its resources in the Workbench database.
        // This is for auditing purposes and potentially workspace restore.
        // TODO: do we want to delete workspace resource references and save only metadata?
        var dbWorkspace: org.pmiops.workbench.db.model.Workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)
        // This automatically handles access control to the workspace.
        fireCloudService.deleteWorkspace(workspaceNamespace, workspaceId)
        dbWorkspace.workspaceActiveStatusEnum = WorkspaceActiveStatus.DELETED
        dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace)
        workspaceService.maybeDeleteRecentWorkspace(dbWorkspace.workspaceId)
        workspaceAuditAdapterService.fireDeleteAction(dbWorkspace)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<WorkspaceResponse> {
        return ResponseEntity.ok<WorkspaceResponse>(workspaceService.getWorkspace(workspaceNamespace, workspaceId))
    }

    fun updateWorkspace(
            workspaceNamespace: String, workspaceId: String, request: UpdateWorkspaceRequest): ResponseEntity<Workspace> {
        var dbWorkspace: org.pmiops.workbench.db.model.Workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER)
        val workspace = request.getWorkspace()
        val fcWorkspace = fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getWorkspace()
        if (workspace == null) {
            throw BadRequestException("No workspace provided in request")
        }
        if (Strings.isNullOrEmpty(workspace!!.getEtag())) {
            throw BadRequestException("Missing required update field 'etag'")
        }
        val version = Etags.toVersion(workspace!!.getEtag())
        if (dbWorkspace.version != version) {
            throw ConflictException("Attempted to modify outdated workspace version")
        }
        if (workspace!!.getDataAccessLevel() != null && !dbWorkspace.dataAccessLevelEnum.equals(workspace!!.getDataAccessLevel())) {
            throw BadRequestException("Attempted to change data access level")
        }
        if (workspace!!.getName() != null) {
            dbWorkspace.name = workspace!!.getName()
        }
        val researchPurpose = request.getWorkspace().getResearchPurpose()
        if (researchPurpose != null) {
            WorkspaceConversionUtils.setResearchPurposeDetails(dbWorkspace, researchPurpose!!)
            if (researchPurpose!!.getReviewRequested()) {
                val now = Timestamp(clock.instant().toEpochMilli())
                dbWorkspace.timeRequested = now
            }
            dbWorkspace.reviewRequested = researchPurpose!!.getReviewRequested()
        }
        // The version asserted on save is the same as the one we read via
        // getRequired() above, see RW-215 for details.
        dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace)
        return ResponseEntity.ok<Workspace>(WorkspaceConversionUtils.toApiWorkspace(dbWorkspace, fcWorkspace))
    }

    fun cloneWorkspace(
            fromWorkspaceNamespace: String, fromWorkspaceId: String, body: CloneWorkspaceRequest): ResponseEntity<CloneWorkspaceResponse> {
        val toWorkspace = body.getWorkspace()
        if (Strings.isNullOrEmpty(toWorkspace.getName())) {
            throw BadRequestException("missing required field 'workspace.name'")
        } else if (toWorkspace.getResearchPurpose() == null) {
            throw BadRequestException("missing required field 'workspace.researchPurpose'")
        }

        val user = userProvider!!.get()

        val toWorkspaceName: String?
        val bufferedBillingProject: BillingProjectBufferEntry
        try {
            bufferedBillingProject = billingProjectBufferService.assignBillingProject(user)
        } catch (e: EmptyBufferException) {
            throw TooManyRequestsException()
        }

        toWorkspaceName = bufferedBillingProject.fireCloudProjectName

        // Retrieving the workspace is done first, which acts as an access check.
        val fromBucket = fireCloudService
                .getWorkspace(fromWorkspaceNamespace, fromWorkspaceId)
                .getWorkspace()
                .getBucketName()

        val fromWorkspace = workspaceService.getRequiredWithCohorts(fromWorkspaceNamespace, fromWorkspaceId)
                ?: throw NotFoundException(
                        String.format("Workspace %s/%s not found", fromWorkspaceNamespace, fromWorkspaceId))

        val toFcWorkspaceId = generateFirecloudWorkspaceId(toWorkspaceName, toWorkspace.getName())
        val toFcWorkspace = fireCloudService.cloneWorkspace(
                fromWorkspaceNamespace,
                fromWorkspaceId,
                toFcWorkspaceId.workspaceNamespace,
                toFcWorkspaceId.workspaceName)

        // In the future, we may want to allow callers to specify whether files
        // should be cloned at all (by default, yes), else they are currently stuck
        // if someone accidentally adds a large file or if there are too many to
        // feasibly copy within a single API request.
        for (b in cloudStorageService.getBlobList(fromBucket)) {
            if (b.size != null && b.size!! / 1e6 > MAX_CLONE_FILE_SIZE_MB) {
                throw FailedPreconditionException(
                        String.format(
                                "workspace %s/%s contains a file larger than %dMB: '%s'; cannot clone - please " + "remove this file, reduce its size, or contact the workspace owner",
                                fromWorkspaceNamespace, fromWorkspaceId, MAX_CLONE_FILE_SIZE_MB, b.name))
            }

            try {
                retryer.call { copyBlob(toFcWorkspace.getBucketName(), b) }
            } catch (e: RetryException) {
                log.log(
                        Level.SEVERE,
                        "Could not copy notebooks into new workspace's bucket " + toFcWorkspace.getBucketName())
                throw WorkbenchException(e)
            } catch (e: ExecutionException) {
                log.log(Level.SEVERE, "Could not copy notebooks into new workspace's bucket " + toFcWorkspace.getBucketName())
                throw WorkbenchException(e)
            }

        }

        // The final step in the process is to clone the AoU representation of the
        // workspace. The implication here is that we may generate orphaned
        // Firecloud workspaces / buckets, but a user should not be able to see
        // half-way cloned workspaces via AoU - so it will just appear as a
        // transient failure.
        val dbWorkspace = org.pmiops.workbench.db.model.Workspace()

        val now = Timestamp(clock.instant().toEpochMilli())
        setDbWorkspaceFields(dbWorkspace, user, toFcWorkspaceId, toFcWorkspace, now)

        dbWorkspace.name = body.getWorkspace().getName()
        val researchPurpose = body.getWorkspace().getResearchPurpose()
        WorkspaceConversionUtils.setResearchPurposeDetails(dbWorkspace, researchPurpose)
        if (researchPurpose.getReviewRequested()) {
            // Use a consistent timestamp.
            dbWorkspace.timeRequested = now
        }
        dbWorkspace.reviewRequested = researchPurpose.getReviewRequested()

        // Clone CDR version from the source, by default.
        val reqCdrVersionId = body.getWorkspace().getCdrVersionId()
        if (Strings.isNullOrEmpty(reqCdrVersionId) || reqCdrVersionId == java.lang.Long.toString(fromWorkspace.cdrVersion!!.cdrVersionId)) {
            dbWorkspace.cdrVersion = fromWorkspace.cdrVersion
            dbWorkspace.dataAccessLevel = fromWorkspace.dataAccessLevel
        } else {
            val reqCdrVersion = setLiveCdrVersionId(dbWorkspace, reqCdrVersionId)
            dbWorkspace.dataAccessLevelEnum = reqCdrVersion.dataAccessLevelEnum
        }

        dbWorkspace.billingMigrationStatusEnum = BillingMigrationStatus.NEW

        var savedWorkspace: org.pmiops.workbench.db.model.Workspace = workspaceService.saveAndCloneCohortsConceptSetsAndDataSets(fromWorkspace, dbWorkspace)

        if (Optional.ofNullable(body.getIncludeUserRoles()).orElse(false)) {
            val fromAclsMap = workspaceService.getFirecloudWorkspaceAcls(
                    fromWorkspace.workspaceNamespace, fromWorkspace.firecloudName)

            val clonedRoles = HashMap<String, WorkspaceAccessLevel>()
            for ((key, value) in fromAclsMap) {
                if (key != user.email) {
                    clonedRoles[key] = WorkspaceAccessLevel.fromValue(value.getAccessLevel())
                } else {
                    clonedRoles[key] = WorkspaceAccessLevel.OWNER
                }
            }
            savedWorkspace = workspaceService.updateWorkspaceAcls(
                    savedWorkspace, clonedRoles, registeredUserDomainEmail)
        }
        workspaceAuditAdapterService.fireDuplicateAction(fromWorkspace, savedWorkspace)
        return ResponseEntity.ok(
                CloneWorkspaceResponse()
                        .workspace(WorkspaceConversionUtils.toApiWorkspace(savedWorkspace, toFcWorkspace)))
    }

    // A retry period is needed because the permission to copy files into the cloned workspace is not
    // granted transactionally
    private fun copyBlob(bucketName: String, b: Blob): Boolean {
        try {
            cloudStorageService.copyBlob(b.blobId, BlobId.of(bucketName, b.name))
            return true
        } catch (e: StorageException) {
            log.warning("Service Account does not have access to bucket $bucketName")
            throw e
        }

    }

    fun shareWorkspace(
            workspaceNamespace: String, workspaceId: String, request: ShareWorkspaceRequest): ResponseEntity<WorkspaceUserRolesResponse> {
        if (Strings.isNullOrEmpty(request.getWorkspaceEtag())) {
            throw BadRequestException("Missing required update field 'workspaceEtag'")
        }

        var dbWorkspace: org.pmiops.workbench.db.model.Workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)
        val version = Etags.toVersion(request.getWorkspaceEtag())
        if (dbWorkspace.version != version) {
            throw ConflictException("Attempted to modify user roles with outdated workspace etag")
        }

        val shareRolesMapBuilder = ImmutableMap.Builder<String, WorkspaceAccessLevel>()
        val aclStringsByUserIdBuilder = ImmutableMap.Builder<Long, String>()

        for (role in request.getItems()) {
            if (role.getRole() == null || role.getRole().toString().trim().isEmpty()) {
                throw BadRequestException("Role required.")
            }
            val invitedUser = userDao.findUserByEmail(role.getEmail())
                    ?: throw BadRequestException(String.format("User %s doesn't exist", role.getEmail()))

            aclStringsByUserIdBuilder.put(invitedUser.userId, role.getRole().toString())
            shareRolesMapBuilder.put(role.getEmail(), role.getRole())
        }
        val aclsByEmail = shareRolesMapBuilder.build()

        // This automatically enforces the "canShare" permission.
        dbWorkspace = workspaceService.updateWorkspaceAcls(
                dbWorkspace, aclsByEmail, registeredUserDomainEmail)
        val resp = WorkspaceUserRolesResponse()
        resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.version))

        val updatedWsAcls = workspaceService.getFirecloudWorkspaceAcls(
                workspaceNamespace, dbWorkspace.firecloudName)
        val updatedUserRoles = workspaceService.convertWorkspaceAclsToUserRoles(updatedWsAcls)
        resp.setItems(updatedUserRoles)

        workspaceAuditAdapterService.fireCollaborateAction(
                dbWorkspace.workspaceId, aclStringsByUserIdBuilder.build())
        return ResponseEntity.ok<WorkspaceUserRolesResponse>(resp)
    }

    /** Record approval or rejection of research purpose.  */
    @AuthorityRequired(Authority.REVIEW_RESEARCH_PURPOSE)
    fun reviewWorkspace(
            ns: String, id: String, review: ResearchPurposeReviewRequest): ResponseEntity<EmptyResponse> {
        val workspace = workspaceService.get(ns, id)
        userService.logAdminWorkspaceAction(
                workspace.workspaceId,
                "research purpose approval",
                workspace.approved,
                review.getApproved())
        workspaceService.setResearchPurposeApproved(ns, id, review.getApproved())
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getNoteBookList(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<List<FileDetail>> {
        val fileList = notebooksService.getNotebooks(workspaceNamespace, workspaceId)
        return ResponseEntity.ok<List<FileDetail>>(fileList)
    }

    fun deleteNotebook(
            workspace: String, workspaceName: String, notebookName: String): ResponseEntity<EmptyResponse> {
        notebooksService.deleteNotebook(workspace, workspaceName, notebookName)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun copyNotebook(
            fromWorkspaceNamespace: String,
            fromWorkspaceId: String,
            fromNotebookName: String,
            copyRequest: CopyRequest): ResponseEntity<FileDetail> {
        val fileDetail: FileDetail
        try {
            fileDetail = notebooksService.copyNotebook(
                    fromWorkspaceNamespace,
                    fromWorkspaceId,
                    NotebooksService.withNotebookExtension(fromNotebookName),
                    copyRequest.getToWorkspaceNamespace(),
                    copyRequest.getToWorkspaceName(),
                    NotebooksService.withNotebookExtension(copyRequest.getNewName()))
        } catch (e: BlobAlreadyExistsException) {
            throw ConflictException("File already exists at copy destination")
        }

        return ResponseEntity.ok<FileDetail>(fileDetail)
    }

    fun cloneNotebook(
            workspace: String, workspaceName: String, notebookName: String): ResponseEntity<FileDetail> {
        val fileDetail: FileDetail
        try {
            fileDetail = notebooksService.cloneNotebook(workspace, workspaceName, notebookName)
        } catch (e: BlobAlreadyExistsException) {
            throw BadRequestException("File already exists at copy destination")
        }

        return ResponseEntity.ok<FileDetail>(fileDetail)
    }

    fun renameNotebook(
            workspace: String, workspaceName: String, rename: NotebookRename): ResponseEntity<FileDetail> {
        val fileDetail: FileDetail
        try {
            fileDetail = notebooksService.renameNotebook(
                    workspace, workspaceName, rename.getName(), rename.getNewName())
        } catch (e: BlobAlreadyExistsException) {
            throw BadRequestException("File already exists at copy destination")
        }

        return ResponseEntity.ok<FileDetail>(fileDetail)
    }

    fun readOnlyNotebook(
            workspaceNamespace: String, workspaceName: String, notebookName: String): ResponseEntity<ReadOnlyNotebookResponse> {
        val response = ReadOnlyNotebookResponse()
                .html(
                        notebooksService.getReadOnlyHtml(workspaceNamespace, workspaceName, notebookName))
        return ResponseEntity.ok<ReadOnlyNotebookResponse>(response)
    }

    fun getNotebookLockingMetadata(
            workspaceNamespace: String, workspaceName: String, notebookName: String): ResponseEntity<NotebookLockingMetadataResponse> {

        // Retrieving the workspace is done first, which acts as an access check.
        val bucketName = fireCloudService
                .getWorkspace(workspaceNamespace, workspaceName)
                .getWorkspace()
                .getBucketName()

        // response may be empty - fill in what we can
        val response = NotebookLockingMetadataResponse()

        // throws NotFoundException if the notebook is not in GCS
        // returns null if found but no user-metadata
        val metadata = cloudStorageService.getMetadata(bucketName, "notebooks/$notebookName")

        if (metadata != null) {
            val lockExpirationTime = metadata["lockExpiresAt"]
            if (lockExpirationTime != null) {
                response.lockExpirationTime(java.lang.Long.valueOf(lockExpirationTime))
            }

            // stored as a SHA-256 hash of bucketName:userEmail
            val lastLockedByHash = metadata["lastLockedBy"]
            if (lastLockedByHash != null) {

                // the caller should not necessarily know the identities of all notebook users
                // so we check against the set of users of this workspace which are known to the caller

                // NOTE: currently, users of workspace X of any access level can see all other
                // workspace X users. This is not desired.
                // https://precisionmedicineinitiative.atlassian.net/browse/RW-3094

                val workspaceUsers = workspaceService.getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName).keys

                response.lastLockedBy(findHashedUser(bucketName, workspaceUsers, lastLockedByHash))
            }
        }

        return ResponseEntity.ok<NotebookLockingMetadataResponse>(response)
    }

    private fun findHashedUser(bucket: String, workspaceUsers: Set<String>, hash: String): String {
        return workspaceUsers.stream()
                .filter { email -> notebookLockingEmailHash(bucket, email) == hash }
                .findAny()
                .orElse("UNKNOWN")
    }

    fun getFirecloudWorkspaceUserRoles(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<WorkspaceUserRolesResponse> {
        val dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        val firecloudAcls = workspaceService.getFirecloudWorkspaceAcls(
                workspaceNamespace, dbWorkspace.firecloudName)
        val userRoles = workspaceService.convertWorkspaceAclsToUserRoles(firecloudAcls)
        val resp = WorkspaceUserRolesResponse()
        resp.setItems(userRoles)
        return ResponseEntity.ok<WorkspaceUserRolesResponse>(resp)
    }

    @AuthorityRequired(Authority.FEATURED_WORKSPACE_ADMIN)
    fun publishWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<EmptyResponse> {
        val dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        workspaceService.setPublished(dbWorkspace, registeredUserDomainEmail, true)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    @AuthorityRequired(Authority.FEATURED_WORKSPACE_ADMIN)
    fun unpublishWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<EmptyResponse> {
        val dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        workspaceService.setPublished(dbWorkspace, registeredUserDomainEmail, false)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun updateRecentWorkspaces(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<RecentWorkspaceResponse> {
        val dbWorkspace = workspaceService.get(workspaceNamespace, workspaceId)
        val userRecentWorkspace = workspaceService.updateRecentWorkspaces(dbWorkspace)
        val workspaceAccessLevel = workspaceService.getWorkspaceAccessLevel(workspaceNamespace, workspaceId)

        val recentWorkspaceResponse = RecentWorkspaceResponse()
        val recentWorkspace = WorkspaceConversionUtils.buildRecentWorkspace(
                userRecentWorkspace, dbWorkspace, workspaceAccessLevel)
        recentWorkspaceResponse.add(recentWorkspace)
        return ResponseEntity.ok<RecentWorkspaceResponse>(recentWorkspaceResponse)
    }

    companion object {

        private val log = Logger.getLogger(WorkspacesController::class.java.name)

        private val RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private val NUM_RANDOM_CHARS = 20
        // If we later decide to tune this value, consider moving to the WorkbenchConfig.
        private val MAX_CLONE_FILE_SIZE_MB = 100

        private fun generateRandomChars(candidateChars: String, length: Int): String {
            val sb = StringBuilder()
            val random = Random()
            for (i in 0 until length) {
                sb.append(candidateChars[random.nextInt(candidateChars.length)])
            }
            return sb.toString()
        }

        @VisibleForTesting
        internal fun notebookLockingEmailHash(bucket: String, email: String): String {
            val toHash = String.format("%s:%s", bucket, email)
            try {
                val sha256 = MessageDigest.getInstance("SHA-256")
                val hash = sha256.digest(toHash.toByteArray(StandardCharsets.UTF_8))
                // convert to printable hex text
                return BaseEncoding.base16().lowerCase().encode(hash)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }

        }
    }
}
