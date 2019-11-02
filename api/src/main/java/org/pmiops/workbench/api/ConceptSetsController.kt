package org.pmiops.workbench.api

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.Ordering
import com.google.common.collect.Streams
import java.sql.Timestamp
import java.time.Clock
import java.util.Comparator
import java.util.function.Function
import java.util.stream.Collectors
import javax.inject.Provider
import javax.persistence.OptimisticLockException
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.ConceptSetListResponse
import org.pmiops.workbench.model.CopyRequest
import org.pmiops.workbench.model.CreateConceptSetRequest
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.Surveys
import org.pmiops.workbench.model.UpdateConceptSetRequest
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ConceptSetsController @Autowired
internal constructor(
        private val workspaceService: WorkspaceService,
        private val conceptSetDao: ConceptSetDao,
        private val conceptDao: ConceptDao,
        private val conceptBigQueryService: ConceptBigQueryService,
        private val userRecentResourceService: UserRecentResourceService,
        private var userProvider: Provider<User>?,
        private val clock: Clock) : ConceptSetsApiDelegate {

    @VisibleForTesting
    internal var maxConceptsPerSet: Int = 0

    init {
        this.maxConceptsPerSet = MAX_CONCEPTS_PER_SET
    }

    @VisibleForTesting
    fun setUserProvider(userProvider: Provider<User>) {
        this.userProvider = userProvider
    }

    fun createConceptSet(
            workspaceNamespace: String, workspaceId: String, request: CreateConceptSetRequest): ResponseEntity<ConceptSet> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        if (request.getAddedIds() == null || request.getAddedIds().size() === 0) {
            throw BadRequestException("Cannot create a concept set with no concepts")
        }
        var dbConceptSet: org.pmiops.workbench.db.model.ConceptSet = FROM_CLIENT_CONCEPT_SET.apply(request.getConceptSet())
        val now = Timestamp(clock.instant().toEpochMilli())
        dbConceptSet.creator = userProvider!!.get()
        dbConceptSet.workspaceId = workspace.workspaceId
        dbConceptSet.creationTime = now
        dbConceptSet.lastModifiedTime = now
        dbConceptSet.version = INITIAL_VERSION
        dbConceptSet.participantCount = 0
        if (request.getAddedIds() != null && !request.getAddedIds().isEmpty()) {
            addConceptsToSet(dbConceptSet, request.getAddedIds())
            if (dbConceptSet.conceptIds.size > maxConceptsPerSet) {
                throw BadRequestException("Exceeded $maxConceptsPerSet in concept set")
            }
            val omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME[dbConceptSet.domainEnum]
            dbConceptSet.participantCount = conceptBigQueryService.getParticipantCountForConcepts(
                    omopTable, dbConceptSet.conceptIds)
        }

        try {
            dbConceptSet = conceptSetDao.save(dbConceptSet)
            userRecentResourceService.updateConceptSetEntry(
                    workspace.workspaceId,
                    userProvider!!.get().userId,
                    dbConceptSet.conceptSetId,
                    now)
        } catch (e: DataIntegrityViolationException) {
            throw BadRequestException(
                    String.format(
                            "Concept set \"/%s/%s/%s\" already exists.",
                            workspaceNamespace, workspaceId, dbConceptSet.name))
        }

        return ResponseEntity.ok(toClientConceptSet(dbConceptSet))
    }

    private fun toClientConceptSet(conceptSet: org.pmiops.workbench.db.model.ConceptSet): ConceptSet {
        val result = TO_CLIENT_CONCEPT_SET.apply(conceptSet)
        if (!conceptSet.conceptIds.isEmpty()) {
            val concepts = conceptDao.findAll(conceptSet.conceptIds)
            result.setConcepts(
                    Streams.stream<Concept>(concepts)
                            .map(ConceptsController.TO_CLIENT_CONCEPT)
                            .sorted(CONCEPT_NAME_ORDERING)
                            .collect<R, A>(Collectors.toList<T>()))
        }
        return result
    }

    fun deleteConceptSet(
            workspaceNamespace: String, workspaceId: String, conceptSetId: Long?): ResponseEntity<EmptyResponse> {
        val conceptSet = getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER)
        conceptSetDao.delete(conceptSet.conceptSetId)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getConceptSet(
            workspaceNamespace: String, workspaceId: String, conceptSetId: Long?): ResponseEntity<ConceptSet> {
        val conceptSet = getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.READER)
        return ResponseEntity.ok(toClientConceptSet(conceptSet))
    }

    fun getConceptSetsInWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<ConceptSetListResponse> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val conceptSets = conceptSetDao.findByWorkspaceId(workspace.workspaceId)
        val response = ConceptSetListResponse()
        // Concept sets in the list response will *not* have concepts under them, as this could be
        // a lot of data... you need to open up a concept set to see what concepts are within it.
        response.setItems(
                conceptSets.stream()
                        .map(TO_CLIENT_CONCEPT_SET)
                        .sorted(Comparator.comparing<R, Any> { c -> c.getName() })
                        .collect(Collectors.toList<T>()))
        return ResponseEntity.ok<ConceptSetListResponse>(response)
    }

    fun getSurveyConceptSetsInWorkspace(
            workspaceNamespace: String, workspaceId: String, surveyName: String): ResponseEntity<ConceptSetListResponse> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val surveyId = CommonStorageEnums.surveysToStorage(Surveys.fromValue(surveyName.toUpperCase()))!!
        val conceptSets = conceptSetDao.findByWorkspaceIdAndSurvey(workspace.workspaceId, surveyId)
        val response = ConceptSetListResponse()
        response.setItems(
                conceptSets.stream()
                        .map(TO_CLIENT_CONCEPT_SET)
                        .sorted(Comparator.comparing<R, Any> { c -> c.getName() })
                        .collect(Collectors.toList<T>()))
        return ResponseEntity.ok<ConceptSetListResponse>(response)
    }

    fun updateConceptSet(
            workspaceNamespace: String, workspaceId: String, conceptSetId: Long?, conceptSet: ConceptSet): ResponseEntity<ConceptSet> {
        var dbConceptSet = getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER)
        if (Strings.isNullOrEmpty(conceptSet.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(conceptSet.getEtag())
        if (dbConceptSet.version != version) {
            throw ConflictException("Attempted to modify outdated concept set version")
        }
        if (conceptSet.name != null) {
            dbConceptSet.name = conceptSet.name
        }
        if (conceptSet.description != null) {
            dbConceptSet.description = conceptSet.description
        }
        if (conceptSet.domain != null && conceptSet.domain !== dbConceptSet.domainEnum) {
            throw BadRequestException("Cannot modify the domain of an existing concept set")
        }
        val now = Timestamp(clock.instant().toEpochMilli())
        dbConceptSet.lastModifiedTime = now
        try {
            dbConceptSet = conceptSetDao.save(dbConceptSet)
            // TODO: add recent resource entry for concept sets [RW-1129]
        } catch (e: OptimisticLockException) {
            throw ConflictException("Failed due to concurrent concept set modification")
        }

        return ResponseEntity.ok(toClientConceptSet(dbConceptSet))
    }

    private fun addConceptsToSet(
            dbConceptSet: org.pmiops.workbench.db.model.ConceptSet, addedIds: List<Long>) {
        val domainEnum = dbConceptSet.domainEnum
        val concepts = conceptDao.findAll(addedIds)
        val mismatchedConcepts = ImmutableList.copyOf<Concept>(concepts).stream()
                .filter { concept -> concept.getConceptClassId() != CONCEPT_CLASS_ID_QUESTION }
                .filter { concept ->
                    val domain = CommonStorageEnums.domainIdToDomain(concept.getDomainId())
                    !domainEnum.equals(domain)
                }
                .collect<List<org.pmiops.workbench.cdr.model.Concept>, Any>(Collectors.toList<Concept>())
        if (!mismatchedConcepts.isEmpty()) {
            val mismatchedConceptIds = Joiner.on(", ")
                    .join(
                            mismatchedConcepts.stream()
                                    .map { it.conceptId }
                                    .collect<List<Long>, Any>(Collectors.toList()))
            throw BadRequestException(
                    String.format("Concepts [%s] are not in domain %s", mismatchedConceptIds, domainEnum))
        }

        dbConceptSet.conceptIds.addAll(addedIds)
    }

    fun updateConceptSetConcepts(
            workspaceNamespace: String,
            workspaceId: String,
            conceptSetId: Long?,
            request: UpdateConceptSetRequest): ResponseEntity<ConceptSet> {
        var dbConceptSet = getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER)

        val allConceptSetIds = dbConceptSet.conceptIds
        if (request.getAddedIds() != null) {
            allConceptSetIds.addAll(request.getAddedIds())
        }
        val sizeOfAllConceptSetIds = allConceptSetIds.stream().collect<Set<Long>, Any>(Collectors.toSet()).size
        if (request.getRemovedIds() != null && request.getRemovedIds().size() === sizeOfAllConceptSetIds) {
            throw BadRequestException("Concept Set must have at least one concept")
        }

        if (Strings.isNullOrEmpty(request.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(request.getEtag())
        if (dbConceptSet.version != version) {
            throw ConflictException("Attempted to modify outdated concept set version")
        }

        if (request.getAddedIds() != null) {
            addConceptsToSet(dbConceptSet, request.getAddedIds())
        }
        if (request.getRemovedIds() != null) {
            dbConceptSet.conceptIds.removeAll(request.getRemovedIds())
        }
        if (dbConceptSet.conceptIds.size > maxConceptsPerSet) {
            throw BadRequestException("Exceeded $maxConceptsPerSet in concept set")
        }
        if (dbConceptSet.conceptIds.isEmpty()) {
            dbConceptSet.participantCount = 0
        } else {
            val omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME[dbConceptSet.domainEnum]
            dbConceptSet.participantCount = conceptBigQueryService.getParticipantCountForConcepts(
                    omopTable, dbConceptSet.conceptIds)
        }

        val now = Timestamp(clock.instant().toEpochMilli())
        dbConceptSet.lastModifiedTime = now
        try {
            dbConceptSet = conceptSetDao.save(dbConceptSet)
            // TODO: add recent resource entry for concept sets [RW-1129]
        } catch (e: OptimisticLockException) {
            throw ConflictException("Failed due to concurrent concept set modification")
        }

        return ResponseEntity.ok(toClientConceptSet(dbConceptSet))
    }

    fun copyConceptSet(
            fromWorkspaceNamespace: String,
            fromWorkspaceId: String,
            fromConceptSetId: String,
            copyRequest: CopyRequest): ResponseEntity<ConceptSet> {
        val now = Timestamp(clock.instant().toEpochMilli())
        workspaceService.enforceWorkspaceAccessLevel(
                fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER)
        val toWorkspace = workspaceService.get(
                copyRequest.getToWorkspaceNamespace(), copyRequest.getToWorkspaceName())
        val fromWorkspace = workspaceService.get(fromWorkspaceNamespace, fromWorkspaceId)
        workspaceService.enforceWorkspaceAccessLevel(
                toWorkspace.workspaceNamespace,
                toWorkspace.firecloudName,
                WorkspaceAccessLevel.WRITER)
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(toWorkspace.cdrVersion)
        if (toWorkspace.cdrVersion!!.cdrVersionId != fromWorkspace.cdrVersion!!.cdrVersionId) {
            throw BadRequestException(
                    "Target workspace does not have the same CDR version as current workspace")
        }
        val conceptSet = conceptSetDao.findOne(java.lang.Long.valueOf(fromConceptSetId))
                ?: throw NotFoundException(
                        String.format(
                                "Concept set %s does not exist",
                                createResourcePath(fromWorkspaceNamespace, fromWorkspaceId, fromConceptSetId)))
        var newConceptSet = org.pmiops.workbench.db.model.ConceptSet(conceptSet)

        newConceptSet.name = copyRequest.getNewName()
        newConceptSet.creator = userProvider!!.get()
        newConceptSet.workspaceId = toWorkspace.workspaceId
        newConceptSet.creationTime = now
        newConceptSet.lastModifiedTime = now
        newConceptSet.version = INITIAL_VERSION

        try {
            newConceptSet = conceptSetDao.save(newConceptSet)
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException(
                    String.format(
                            "Concept set %s already exists.",
                            createResourcePath(
                                    toWorkspace.workspaceNamespace,
                                    toWorkspace.firecloudName,
                                    newConceptSet.name)))
        }

        userRecentResourceService.updateConceptSetEntry(
                toWorkspace.workspaceId,
                userProvider!!.get().userId,
                newConceptSet.conceptSetId,
                now)
        return ResponseEntity.ok(toClientConceptSet(newConceptSet))
    }

    private fun createResourcePath(
            workspaceNamespace: String?, workspaceFirecloudName: String?, identifier: String?): String {
        return String.format("\"/%s/%s/%s\"", workspaceNamespace, workspaceFirecloudName, identifier)
    }

    private fun getDbConceptSet(
            workspaceNamespace: String,
            workspaceId: String,
            conceptSetId: Long?,
            workspaceAccessLevel: WorkspaceAccessLevel): org.pmiops.workbench.db.model.ConceptSet {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, workspaceAccessLevel)

        val conceptSet = conceptSetDao.findOne(conceptSetId)
        if (conceptSet == null || workspace.workspaceId != conceptSet.workspaceId) {
            throw NotFoundException(
                    String.format(
                            "No concept set with ID %s in workspace %s.",
                            conceptSetId, workspace.firecloudName))
        }
        return conceptSet
    }

    companion object {

        private val MAX_CONCEPTS_PER_SET = 1000
        private val CONCEPT_CLASS_ID_QUESTION = "Question"
        private val INITIAL_VERSION = 1

        internal val TO_CLIENT_CONCEPT_SET: Function<org.pmiops.workbench.db.model.ConceptSet, ConceptSet> = Function<org.pmiops.workbench.db.model.ConceptSet, Any> { conceptSet ->
            val result = ConceptSet()
                    .etag(Etags.fromVersion(conceptSet.version))
                    .lastModifiedTime(conceptSet.lastModifiedTime!!.time)
                    .creationTime(conceptSet.creationTime!!.time)
                    .description(conceptSet.description)
                    .id(conceptSet.conceptSetId)
                    .name(conceptSet.name)
                    .domain(conceptSet.domainEnum)
                    .participantCount(conceptSet.participantCount)
                    .survey(conceptSet.surveysEnum)
            if (conceptSet.creator != null) {
                result.creator(conceptSet.creator!!.email)
            }
            result
        }

        private val FROM_CLIENT_CONCEPT_SET = Function<ConceptSet, ConceptSet> { conceptSet ->
            val dbConceptSet = org.pmiops.workbench.db.model.ConceptSet()
            dbConceptSet.domainEnum = conceptSet.domain
            if (conceptSet.survey != null) {
                dbConceptSet.surveysEnum = conceptSet.survey
            }
            if (dbConceptSet.domainEnum == null) {
                throw BadRequestException(
                        "Domain " + conceptSet.domain + " is not allowed for concept sets")
            }
            if (conceptSet.getEtag() != null) {
                dbConceptSet.version = Etags.toVersion(conceptSet.getEtag())
            }
            dbConceptSet.description = conceptSet.description
            dbConceptSet.name = conceptSet.name
            dbConceptSet
        }

        private val CONCEPT_NAME_ORDERING = Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Function<Concept, String> { Concept.getConceptName() })
    }
}
