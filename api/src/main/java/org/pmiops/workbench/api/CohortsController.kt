package org.pmiops.workbench.api

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import java.sql.Timestamp
import java.time.Clock
import java.util.Comparator
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import javax.persistence.OptimisticLockException
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cohorts.CohortFactory
import org.pmiops.workbench.cohorts.CohortMaterializationService
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.*
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.model.CdrQuery
import org.pmiops.workbench.model.Cohort
import org.pmiops.workbench.model.CohortAnnotationsRequest
import org.pmiops.workbench.model.CohortAnnotationsResponse
import org.pmiops.workbench.model.CohortListResponse
import org.pmiops.workbench.model.DataTableSpecification
import org.pmiops.workbench.model.DuplicateCohortRequest
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.MaterializeCohortRequest
import org.pmiops.workbench.model.MaterializeCohortResponse
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortsController @Autowired
internal constructor(
        private val workspaceService: WorkspaceService,
        private val cohortDao: CohortDao,
        private val cdrVersionDao: CdrVersionDao,
        private val cohortFactory: CohortFactory,
        private val cohortReviewDao: CohortReviewDao,
        private val conceptSetDao: ConceptSetDao,
        private val cohortMaterializationService: CohortMaterializationService,
        private var userProvider: Provider<User>?,
        private val clock: Clock,
        private val cdrVersionService: CdrVersionService,
        private val userRecentResourceService: UserRecentResourceService) : CohortsApiDelegate {

    @VisibleForTesting
    fun setUserProvider(userProvider: Provider<User>) {
        this.userProvider = userProvider
    }

    private fun checkForDuplicateCohortNameException(newCohortName: String, workspace: Workspace) {
        if (cohortDao.findCohortByNameAndWorkspaceId(newCohortName, workspace.workspaceId) != null) {
            throw BadRequestException(
                    String.format(
                            "Cohort \"/%s/%s/%s\" already exists.",
                            workspace.workspaceNamespace, workspace.workspaceId, newCohortName))
        }
    }

    fun createCohort(
            workspaceNamespace: String, workspaceId: String, cohort: Cohort): ResponseEntity<Cohort> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        val workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        checkForDuplicateCohortNameException(cohort.name, workspace)

        var newCohort: org.pmiops.workbench.db.model.Cohort = cohortFactory.createCohort(cohort, userProvider!!.get(), workspace.workspaceId)
        try {
            // TODO Make this a pre-check within a transaction?
            newCohort = cohortDao.save(newCohort)
            userRecentResourceService.updateCohortEntry(
                    workspace.workspaceId,
                    userProvider!!.get().userId,
                    newCohort.cohortId,
                    newCohort.lastModifiedTime)
        } catch (e: DataIntegrityViolationException) {
            // TODO The exception message doesn't show up anywhere; neither logged nor returned to the
            // client by Spring (the client gets a default reason string).
            throw ServerErrorException(
                    String.format(
                            "Could not save Cohort (\"/%s/%s/%s\")",
                            workspace.workspaceNamespace, workspace.workspaceId, newCohort.name),
                    e)
        }

        return ResponseEntity.ok(TO_CLIENT_COHORT.apply(newCohort))
    }

    fun duplicateCohort(
            workspaceNamespace: String, workspaceId: String, params: DuplicateCohortRequest): ResponseEntity<Cohort> {
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        val workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        checkForDuplicateCohortNameException(params.getNewName(), workspace)

        val originalCohort = getDbCohort(workspaceNamespace, workspaceId, params.getOriginalCohortId())
        var newCohort: org.pmiops.workbench.db.model.Cohort = cohortFactory.duplicateCohort(
                params.getNewName(), userProvider!!.get(), workspace, originalCohort)
        try {
            newCohort = cohortDao.save(newCohort)
            userRecentResourceService.updateCohortEntry(
                    workspace.workspaceId,
                    userProvider!!.get().userId,
                    newCohort.cohortId,
                    Timestamp(clock.instant().toEpochMilli()))
        } catch (e: Exception) {
            throw ServerErrorException(
                    String.format(
                            "Could not save Cohort (\"/%s/%s/%s\")",
                            workspace.workspaceNamespace, workspace.workspaceId, newCohort.name),
                    e)
        }

        return ResponseEntity.ok(TO_CLIENT_COHORT.apply(newCohort))
    }

    fun deleteCohort(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?): ResponseEntity<EmptyResponse> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId)
        cohortDao.delete(dbCohort)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getCohort(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?): ResponseEntity<Cohort> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId)
        return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort))
    }

    fun getCohortsInWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<CohortListResponse> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val workspace = workspaceService.getRequiredWithCohorts(workspaceNamespace, workspaceId)
        val response = CohortListResponse()
        val cohorts = workspace.cohorts
        if (cohorts != null) {
            response.setItems(
                    cohorts.stream()
                            .map(TO_CLIENT_COHORT)
                            .sorted(Comparator.comparing<R, Any> { c -> c.getName() })
                            .collect(Collectors.toList<T>()))
        }
        return ResponseEntity.ok<CohortListResponse>(response)
    }

    fun updateCohort(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?, cohort: Cohort): ResponseEntity<Cohort> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        var dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId)
        if (Strings.isNullOrEmpty(cohort.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(cohort.getEtag())
        if (dbCohort.version != version) {
            throw ConflictException("Attempted to modify outdated cohort version")
        }
        if (cohort.type != null) {
            dbCohort.type = cohort.type
        }
        if (cohort.name != null) {
            dbCohort.name = cohort.name
        }
        if (cohort.description != null) {
            dbCohort.description = cohort.description
        }
        if (cohort.criteria != null) {
            dbCohort.criteria = cohort.criteria
        }
        val now = Timestamp(clock.instant().toEpochMilli())
        dbCohort.lastModifiedTime = now
        try {
            // The version asserted on save is the same as the one we read via
            // getRequired() above, see RW-215 for details.
            dbCohort = cohortDao.save(dbCohort)
        } catch (e: OptimisticLockException) {
            log.log(Level.WARNING, "version conflict for cohort update", e)
            throw ConflictException("Failed due to concurrent cohort modification")
        }

        return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort))
    }

    private fun getConceptIds(workspace: Workspace, tableQuery: TableQuery): Set<Long>? {
        val conceptSetName = tableQuery.getConceptSetName()
        if (conceptSetName != null) {
            val conceptSet = conceptSetDao.findConceptSetByNameAndWorkspaceId(
                    conceptSetName, workspace.workspaceId)
                    ?: throw NotFoundException(
                            String.format(
                                    "Couldn't find concept set with name %s in workspace %s/%s",
                                    conceptSetName, workspace.workspaceNamespace, workspace.workspaceId))
            val tableName = ConceptSetDao.DOMAIN_TO_TABLE_NAME[conceptSet.domainEnum]
                    ?: throw ServerErrorException(
                            "Couldn't find table for domain: " + conceptSet.domainEnum)
            if (tableName != tableQuery.getTableName()) {
                throw BadRequestException(
                        String.format(
                                "Can't use concept set for domain %s with table %s",
                                conceptSet.domainEnum, tableQuery.getTableName()))
            }
            return conceptSet.conceptIds
        }
        return null
    }

    fun materializeCohort(
            workspaceNamespace: String, workspaceId: String, request: MaterializeCohortRequest): ResponseEntity<MaterializeCohortResponse> {
        // This also enforces registered auth domain.
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        var cdrVersion = workspace.cdrVersion

        if (request.getCdrVersionName() != null) {
            cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName())
            if (cdrVersion == null) {
                throw NotFoundException(
                        String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()))
            }
            cdrVersionService.setCdrVersion(cdrVersion)
        }
        val cohortSpec: String?
        var cohortReview: CohortReview? = null
        if (request.getCohortName() != null) {
            val cohort = cohortDao.findCohortByNameAndWorkspaceId(
                    request.getCohortName(), workspace.workspaceId)
                    ?: throw NotFoundException(
                            String.format(
                                    "Couldn't find cohort with name %s in workspace %s/%s",
                                    request.getCohortName(), workspaceNamespace, workspaceId))
            cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
                    cohort.cohortId, cdrVersion!!.cdrVersionId)
            cohortSpec = cohort.criteria
        } else if (request.getCohortSpec() != null) {
            cohortSpec = request.getCohortSpec()
            if (request.getStatusFilter() != null) {
                throw BadRequestException("statusFilter cannot be used with cohortSpec")
            }
        } else {
            throw BadRequestException("Must specify either cohortName or cohortSpec")
        }
        var conceptIds: Set<Long>? = null
        if (request.getFieldSet() != null && request.getFieldSet().getTableQuery() != null) {
            conceptIds = getConceptIds(workspace, request.getFieldSet().getTableQuery())
        }

        val pageSize = request.getPageSize()
        if (pageSize == null || pageSize == 0) {
            request.setPageSize(DEFAULT_PAGE_SIZE)
        } else if (pageSize < 0) {
            throw BadRequestException(
                    String.format(
                            "Invalid page size: %s; must be between 1 and %d", pageSize, MAX_PAGE_SIZE))
        } else if (pageSize > MAX_PAGE_SIZE) {
            request.setPageSize(MAX_PAGE_SIZE)
        }

        val response = cohortMaterializationService.materializeCohort(
                cohortReview, cohortSpec, conceptIds, request)
        return ResponseEntity.ok<MaterializeCohortResponse>(response)
    }

    fun getDataTableQuery(
            workspaceNamespace: String, workspaceId: String, request: DataTableSpecification): ResponseEntity<CdrQuery> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        var cdrVersion = workspace.cdrVersion

        if (request.getCdrVersionName() != null) {
            cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName())
            if (cdrVersion == null) {
                throw NotFoundException(
                        String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()))
            }
            cdrVersionService.setCdrVersion(cdrVersion)
        }
        val cohortSpec: String?
        var cohortReview: CohortReview? = null
        if (request.getCohortName() != null) {
            val cohort = cohortDao.findCohortByNameAndWorkspaceId(
                    request.getCohortName(), workspace.workspaceId)
                    ?: throw NotFoundException(
                            String.format(
                                    "Couldn't find cohort with name %s in workspace %s/%s",
                                    request.getCohortName(), workspaceNamespace, workspaceId))
            cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
                    cohort.cohortId, cdrVersion!!.cdrVersionId)
            cohortSpec = cohort.criteria
        } else if (request.getCohortSpec() != null) {
            cohortSpec = request.getCohortSpec()
            if (request.getStatusFilter() != null) {
                throw BadRequestException("statusFilter cannot be used with cohortSpec")
            }
        } else {
            throw BadRequestException("Must specify either cohortName or cohortSpec")
        }
        val conceptIds = getConceptIds(workspace, request.getTableQuery())
        val query = cohortMaterializationService.getCdrQuery(cohortSpec, request, cohortReview, conceptIds)
        return ResponseEntity.ok<CdrQuery>(query)
    }

    fun getCohortAnnotations(
            workspaceNamespace: String, workspaceId: String, request: CohortAnnotationsRequest): ResponseEntity<CohortAnnotationsResponse> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        var cdrVersion = workspace.cdrVersion
        if (request.getCdrVersionName() != null) {
            cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName())
            if (cdrVersion == null) {
                throw NotFoundException(
                        String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()))
            }
        }
        val cohort = cohortDao.findCohortByNameAndWorkspaceId(
                request.getCohortName(), workspace.workspaceId)
                ?: throw NotFoundException(
                        String.format(
                                "Couldn't find cohort with name %s in workspace %s/%s",
                                request.getCohortName(), workspaceNamespace, workspaceId))
        val cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
                cohort.cohortId, cdrVersion!!.cdrVersionId)
                ?: return ResponseEntity.ok(
                        CohortAnnotationsResponse().columns(request.getAnnotationQuery().getColumns()))
        return ResponseEntity.ok<CohortAnnotationsResponse>(cohortMaterializationService.getAnnotations(cohortReview, request))
    }

    private fun getDbCohort(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?): org.pmiops.workbench.db.model.Cohort {
        val workspace = workspaceService.getRequired(workspaceNamespace, workspaceId)

        val cohort = cohortDao.findOne(cohortId)
        if (cohort == null || cohort.workspaceId != workspace.workspaceId) {
            throw NotFoundException(
                    String.format(
                            "No cohort with name %s in workspace %s.", cohortId, workspace.firecloudName))
        }
        return cohort
    }

    companion object {

        @VisibleForTesting
        internal val MAX_PAGE_SIZE = 10000
        @VisibleForTesting
        internal val DEFAULT_PAGE_SIZE = 1000
        private val log = Logger.getLogger(CohortsController::class.java.name)

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        internal val TO_CLIENT_COHORT: Function<org.pmiops.workbench.db.model.Cohort, Cohort> = Function<org.pmiops.workbench.db.model.Cohort, Any> { cohort ->
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
            result
        }
    }
}
