package org.pmiops.workbench.api

import org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT
import org.pmiops.workbench.model.FilterColumns.ANSWER
import org.pmiops.workbench.model.FilterColumns.DOMAIN
import org.pmiops.workbench.model.FilterColumns.DOSE
import org.pmiops.workbench.model.FilterColumns.FIRST_MENTION
import org.pmiops.workbench.model.FilterColumns.LAST_MENTION
import org.pmiops.workbench.model.FilterColumns.NUM_OF_MENTIONS
import org.pmiops.workbench.model.FilterColumns.QUESTION
import org.pmiops.workbench.model.FilterColumns.REF_RANGE
import org.pmiops.workbench.model.FilterColumns.ROUTE
import org.pmiops.workbench.model.FilterColumns.SOURCE_CODE
import org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID
import org.pmiops.workbench.model.FilterColumns.SOURCE_NAME
import org.pmiops.workbench.model.FilterColumns.SOURCE_VOCAB
import org.pmiops.workbench.model.FilterColumns.STANDARD_CODE
import org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID
import org.pmiops.workbench.model.FilterColumns.STANDARD_NAME
import org.pmiops.workbench.model.FilterColumns.STANDARD_VOCAB
import org.pmiops.workbench.model.FilterColumns.START_DATE
import org.pmiops.workbench.model.FilterColumns.STRENGTH
import org.pmiops.workbench.model.FilterColumns.SURVEY_NAME
import org.pmiops.workbench.model.FilterColumns.UNIT
import org.pmiops.workbench.model.FilterColumns.VAL_AS_NUMBER
import org.pmiops.workbench.model.FilterColumns.VISIT_TYPE

import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.TableResult
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import java.sql.Date
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.HashMap
import java.util.Optional
import java.util.function.BiFunction
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import javax.persistence.OptimisticLockException
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria
import org.pmiops.workbench.cohortreview.CohortReviewService
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder
import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.model.CohortChartData
import org.pmiops.workbench.model.CohortChartDataListResponse
import org.pmiops.workbench.model.CohortReviewListResponse
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.ConceptIdName
import org.pmiops.workbench.model.CreateReviewRequest
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.Filter
import org.pmiops.workbench.model.FilterColumns
import org.pmiops.workbench.model.ModifyCohortStatusRequest
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest
import org.pmiops.workbench.model.PageFilterRequest
import org.pmiops.workbench.model.ParticipantChartData
import org.pmiops.workbench.model.ParticipantChartDataListResponse
import org.pmiops.workbench.model.ParticipantCohortAnnotation
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse
import org.pmiops.workbench.model.ParticipantData
import org.pmiops.workbench.model.ParticipantDataListResponse
import org.pmiops.workbench.model.ReviewStatus
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.SortOrder
import org.pmiops.workbench.model.Vocabulary
import org.pmiops.workbench.model.VocabularyListResponse
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import kotlin.String.Companion

@RestController
class CohortReviewController @Autowired
internal constructor(
        private val cbCriteriaDao: CBCriteriaDao,
        private val cohortReviewService: CohortReviewService,
        private val bigQueryService: BigQueryService,
        private val cohortQueryBuilder: CohortQueryBuilder,
        private val reviewQueryBuilder: ReviewQueryBuilder,
        private val userRecentResourceService: UserRecentResourceService,
        private var userProvider: Provider<User>?,
        private val clock: Clock) : CohortReviewApiDelegate {

    @VisibleForTesting
    fun setUserProvider(userProvider: Provider<User>) {
        this.userProvider = userProvider
    }

    /**
     * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If
     * participant cohort status data exists for a review or no cohort review exists for
     * cohortReviewId then throw a [BadRequestException].
     *
     * @param workspaceNamespace
     * @param workspaceId
     * @param cohortId
     * @param cdrVersionId
     * @param request
     */
    fun createCohortReview(
            workspaceNamespace: String,
            workspaceId: String,
            cohortId: Long?,
            cdrVersionId: Long?,
            request: CreateReviewRequest): ResponseEntity<org.pmiops.workbench.model.CohortReview> {
        if (request.getSize() <= 0 || request.getSize() > MAX_REVIEW_SIZE) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE))
        }

        val cohort = cohortReviewService.findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.WRITER)
        var cohortReview: CohortReview? = null
        try {
            cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId)
        } catch (nfe: NotFoundException) {
            cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider!!.get())
            cohortReviewService.saveCohortReview(cohortReview)
        }

        if (cohortReview!!.reviewSize > 0) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId))
        }

        val searchRequest = Gson().fromJson<Any>(getCohortDefinition(cohort), SearchRequest::class.java)

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(
                        cohortQueryBuilder.buildRandomParticipantQuery(
                                ParticipantCriteria(searchRequest), request.getSize(), 0L)))
        val rm = bigQueryService.getResultMapper(result)

        val participantCohortStatuses = createParticipantCohortStatusesList(cohortReview.cohortReviewId, result, rm)

        cohortReview
                .reviewSize(participantCohortStatuses.size.toLong())
                .reviewStatusEnum(ReviewStatus.CREATED)

        // when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
        cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses)

        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())

        val paginatedPCS = cohortReviewService.findAll(cohortReview.cohortReviewId, pageRequest)
        lookupGenderRaceEthnicityValues(paginatedPCS)

        val responseReview = TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest)
        responseReview.setParticipantCohortStatuses(
                paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<org.pmiops.workbench.model.CohortReview>(responseReview)
    }

    fun createParticipantCohortAnnotation(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            request: ParticipantCohortAnnotation): ResponseEntity<ParticipantCohortAnnotation> {

        if (!request.getCohortReviewId().equals(cohortReviewId)) {
            throw BadRequestException(
                    "Bad Request: request cohort review id must equal path parameter cohort review id.")
        }

        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        var participantCohortAnnotation: org.pmiops.workbench.db.model.ParticipantCohortAnnotation = FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(request)

        participantCohortAnnotation = cohortReviewService.saveParticipantCohortAnnotation(
                request.getCohortReviewId(), participantCohortAnnotation)

        return ResponseEntity.ok<ParticipantCohortAnnotation>(
                TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation))
    }

    fun deleteCohortReview(
            workspaceNamespace: String, workspaceId: String, cohortReviewId: Long?): ResponseEntity<EmptyResponse> {
        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val dbCohortReview = cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId)
        cohortReviewService.deleteCohortReview(dbCohortReview)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun deleteParticipantCohortAnnotation(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            annotationId: Long?): ResponseEntity<EmptyResponse> {

        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        // will throw a NotFoundException if participant cohort annotation does not exist
        cohortReviewService.deleteParticipantCohortAnnotation(
                annotationId, cohortReviewId, participantId)

        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getCohortChartData(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            domain: String,
            limit: Int?): ResponseEntity<CohortChartDataListResponse> {
        val chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT)
        if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Please provide a chart limit between %d and %d.",
                            MIN_LIMIT, MAX_LIMIT))
        }

        val cohortReview = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(cohortReview.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)

        val searchRequest = Gson().fromJson<Any>(getCohortDefinition(cohort), SearchRequest::class.java)

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(
                        cohortQueryBuilder.buildDomainChartInfoCounterQuery(
                                ParticipantCriteria(searchRequest),
                                DomainType.fromValue(domain),
                                chartLimit)))
        val rm = bigQueryService.getResultMapper(result)

        val response = CohortChartDataListResponse()
        response.count(cohortReview.matchedParticipantCount)
        for (row in result.iterateAll()) {
            response.addItemsItem(
                    CohortChartData()
                            .name(bigQueryService.getString(row, rm["name"]))
                            .conceptId(bigQueryService.getLong(row, rm["conceptId"]))
                            .count(bigQueryService.getLong(row, rm["count"])))
        }

        return ResponseEntity.ok<CohortChartDataListResponse>(response)
    }

    fun getCohortReviewsInWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<CohortReviewListResponse> {
        // This also enforces registered auth domain.
        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val reviews = cohortReviewService.getRequiredWithCohortReviews(workspaceNamespace, workspaceId)
        val response = CohortReviewListResponse()
        response.setItems(reviews.stream().map(TO_CLIENT_COHORTREVIEW).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<CohortReviewListResponse>(response)
    }

    fun getParticipantChartData(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            domain: String,
            limit: Int?): ResponseEntity<ParticipantChartDataListResponse> {
        val chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT)
        if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Please provide a chart limit between %d and %d.",
                            MIN_LIMIT, MAX_LIMIT))
        }
        val cohortReview = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(cohortReview.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(
                        reviewQueryBuilder.buildChartDataQuery(
                                participantId, DomainType.fromValue(domain), chartLimit)))
        val rm = bigQueryService.getResultMapper(result)

        val response = ParticipantChartDataListResponse()
        for (row in result.iterateAll()) {
            response.addItemsItem(convertRowToChartData(rm, row))
        }
        return ResponseEntity.ok<ParticipantChartDataListResponse>(response)
    }

    fun getParticipantCohortAnnotations(
            workspaceNamespace: String, workspaceId: String, cohortReviewId: Long?, participantId: Long?): ResponseEntity<ParticipantCohortAnnotationListResponse> {
        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val annotations = cohortReviewService.findParticipantCohortAnnotations(cohortReviewId, participantId)

        val response = ParticipantCohortAnnotationListResponse()
        response.setItems(
                annotations.stream()
                        .map(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION)
                        .collect(Collectors.toList<T>()))
        return ResponseEntity.ok<ParticipantCohortAnnotationListResponse>(response)
    }

    fun getParticipantCohortStatus(
            workspaceNamespace: String, workspaceId: String, cohortReviewId: Long?, participantId: Long?): ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> {
        val review = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(review.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)

        val status = cohortReviewService.findParticipantCohortStatus(review.cohortReviewId, participantId)
        lookupGenderRaceEthnicityValues(Arrays.asList(status))
        return ResponseEntity.ok<org.pmiops.workbench.model.ParticipantCohortStatus>(TO_CLIENT_PARTICIPANT.apply(status))
    }

    /**
     * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
     * based on page, pageSize, sortOrder and sortColumn.
     */
    fun getParticipantCohortStatuses(
            workspaceNamespace: String,
            workspaceId: String,
            cohortId: Long?,
            cdrVersionId: Long?,
            request: PageFilterRequest): ResponseEntity<org.pmiops.workbench.model.CohortReview> {
        var cohortReview: CohortReview? = null
        val cohort = cohortReviewService.findCohort(cohortId!!)

        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)
        try {
            cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId)
        } catch (nfe: NotFoundException) {
            cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider!!.get())
        }

        val pageRequest = createPageRequest(request)
        convertGenderRaceEthnicityFilters(pageRequest)
        convertGenderRaceEthnicitySortOrder(pageRequest)

        val participantCohortStatuses = cohortReviewService.findAll(cohortReview!!.cohortReviewId, pageRequest)
        lookupGenderRaceEthnicityValues(participantCohortStatuses)

        val queryResultSize = if (pageRequest.filters.isEmpty())
            cohortReview.reviewSize
        else
            cohortReviewService.findCount(cohortReview.cohortReviewId, pageRequest)

        val responseReview = TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest)
        responseReview.setParticipantCohortStatuses(
                participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList<T>()))
        responseReview.setQueryResultSize(queryResultSize)
        val now = Timestamp(clock.instant().toEpochMilli())

        userRecentResourceService.updateCohortEntry(
                cohort.workspaceId, userProvider!!.get().userId, cohortId, now)
        return ResponseEntity.ok<org.pmiops.workbench.model.CohortReview>(responseReview)
    }

    fun getParticipantData(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            request: PageFilterRequest): ResponseEntity<ParticipantDataListResponse> {
        val review = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(review.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)

        // this validates that the participant is in the requested review.
        cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)

        val domain = request.getDomain()
        val pageRequest = createPageRequest(request)

        var result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(
                        reviewQueryBuilder.buildQuery(participantId, domain, pageRequest)))
        var rm = bigQueryService.getResultMapper(result)

        val response = ParticipantDataListResponse()
        for (row in result.iterateAll()) {
            response.addItemsItem(convertRowToParticipantData(rm, row, domain))
        }

        if (result.totalRows == pageRequest.pageSize) {
            result = bigQueryService.executeQuery(
                    bigQueryService.filterBigQueryConfig(
                            reviewQueryBuilder.buildCountQuery(participantId, domain, pageRequest)))
            rm = bigQueryService.getResultMapper(result)
            response.count(
                    bigQueryService.getLong(result.iterateAll().iterator().next(), rm["count"]))
        } else {
            response.count(result.totalRows)
        }
        return ResponseEntity.ok<ParticipantDataListResponse>(response)
    }

    fun getVocabularies(
            workspaceNamespace: String, workspaceId: String, cohortReviewId: Long?): ResponseEntity<VocabularyListResponse> {
        val review = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(review.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.READER)

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(reviewQueryBuilder.buildVocabularyDataQuery()))
        val rm = bigQueryService.getResultMapper(result)

        val response = VocabularyListResponse()
        for (row in result.iterateAll()) {
            response.addItemsItem(
                    Vocabulary()
                            .domain(bigQueryService.getString(row, rm["domain"]))
                            .type(bigQueryService.getString(row, rm["type"]))
                            .vocabulary(bigQueryService.getString(row, rm["vocabulary"])))
        }
        return ResponseEntity.ok<VocabularyListResponse>(response)
    }

    fun updateCohortReview(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            cohortReview: org.pmiops.workbench.model.CohortReview): ResponseEntity<org.pmiops.workbench.model.CohortReview> {
        // This also enforces registered auth domain.
        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        val dbCohortReview = cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId)
        if (Strings.isNullOrEmpty(cohortReview.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(cohortReview.getEtag())
        if (dbCohortReview.version != version) {
            throw ConflictException("Attempted to modify outdated cohort review version")
        }
        if (cohortReview.getCohortName() != null) {
            dbCohortReview.cohortName = cohortReview.getCohortName()
        }
        if (cohortReview.getDescription() != null) {
            dbCohortReview.description = cohortReview.getDescription()
        }
        dbCohortReview.lastModifiedTime = Timestamp(clock.instant().toEpochMilli())
        try {
            cohortReviewService.saveCohortReview(dbCohortReview)
        } catch (e: OptimisticLockException) {
            log.log(Level.WARNING, "version conflict for cohort review update", e)
            throw ConflictException("Failed due to concurrent cohort review modification")
        }

        return ResponseEntity.ok<org.pmiops.workbench.model.CohortReview>(TO_CLIENT_COHORTREVIEW.apply(dbCohortReview))
    }

    fun updateParticipantCohortAnnotation(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            annotationId: Long?,
            request: ModifyParticipantCohortAnnotationRequest): ResponseEntity<ParticipantCohortAnnotation> {
        cohortReviewService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val participantCohortAnnotation = cohortReviewService.updateParticipantCohortAnnotation(
                annotationId, cohortReviewId, participantId, request)

        return ResponseEntity.ok<ParticipantCohortAnnotation>(
                TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation))
    }

    fun updateParticipantCohortStatus(
            workspaceNamespace: String,
            workspaceId: String,
            cohortReviewId: Long?,
            participantId: Long?,
            cohortStatusRequest: ModifyCohortStatusRequest): ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> {
        val cohortReview = cohortReviewService.findCohortReview(cohortReviewId)
        val cohort = cohortReviewService.findCohort(cohortReview.cohortId)
        cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
                workspaceNamespace, workspaceId, cohort.workspaceId, WorkspaceAccessLevel.WRITER)

        val participantCohortStatus = cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId)

        participantCohortStatus.statusEnum = cohortStatusRequest.getStatus()
        cohortReviewService.saveParticipantCohortStatus(participantCohortStatus)
        lookupGenderRaceEthnicityValues(Arrays.asList(participantCohortStatus))

        cohortReview.lastModifiedTime(Timestamp(clock.instant().toEpochMilli()))
        cohortReview.incrementReviewedCount()
        cohortReviewService.saveCohortReview(cohortReview)

        return ResponseEntity.ok<org.pmiops.workbench.model.ParticipantCohortStatus>(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus))
    }

    /**
     * Helper method to create a new [CohortReview].
     *
     * @param cdrVersionId
     * @param cohort
     * @param creator
     */
    private fun initializeCohortReview(cdrVersionId: Long?, cohort: Cohort, creator: User): CohortReview {
        val request = Gson().fromJson<Any>(getCohortDefinition(cohort), SearchRequest::class.java)

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(
                        cohortQueryBuilder.buildParticipantCounterQuery(ParticipantCriteria(request))))
        val rm = bigQueryService.getResultMapper(result)
        val row = result.iterateAll().iterator().next()
        val cohortCount = bigQueryService.getLong(row, rm["count"])!!

        return createNewCohortReview(cohort, cdrVersionId, cohortCount, creator)
    }

    /**
     * Helper method that builds a list of [ParticipantCohortStatus] from BigQuery results.
     *
     * @param cohortReviewId
     * @param result
     * @param rm
     * @return
     */
    private fun createParticipantCohortStatusesList(
            cohortReviewId: Long?, result: TableResult, rm: Map<String, Int>): List<ParticipantCohortStatus> {
        val participantCohortStatuses = ArrayList<ParticipantCohortStatus>()
        for (row in result.iterateAll()) {
            val birthDateTimeString = bigQueryService.getString(row, rm["birth_datetime"])
                    ?: throw BigQueryException(
                            500, "birth_datetime is null at position: " + rm["birth_datetime"])
            val birthDate = Date.from(Instant.ofEpochMilli(java.lang.Double.valueOf(birthDateTimeString).toLong() * 1000))
            participantCohortStatuses.add(
                    ParticipantCohortStatus()
                            .participantKey(
                                    ParticipantCohortStatusKey(
                                            cohortReviewId!!, bigQueryService.getLong(row, rm["person_id"])!!))
                            .statusEnum(CohortStatus.NOT_REVIEWED)
                            .birthDate(Date(birthDate.time))
                            .genderConceptId(bigQueryService.getLong(row, rm["gender_concept_id"]))
                            .raceConceptId(bigQueryService.getLong(row, rm["race_concept_id"]))
                            .ethnicityConceptId(bigQueryService.getLong(row, rm["ethnicity_concept_id"]))
                            .deceased(bigQueryService.getBoolean(row, rm["deceased"])!!))
        }
        return participantCohortStatuses
    }

    /**
     * Helper to method that consolidates access to Cohort Definition. Will throw a [ ] if [Cohort.getCriteria] return null.
     *
     * @param cohort
     * @return
     */
    private fun getCohortDefinition(cohort: Cohort): String {
        return cohort.criteria
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: No Cohort definition matching cohortId: %s", cohort.cohortId))
    }

    /**
     * Helper method that constructs a [CohortReview] with the specified ids and count.
     *
     * @param cohort
     * @param cdrVersionId
     * @param cohortCount
     * @param creator
     * @return
     */
    private fun createNewCohortReview(
            cohort: Cohort, cdrVersionId: Long?, cohortCount: Long?, creator: User): CohortReview {
        return CohortReview()
                .cohortId(cohort.cohortId)
                .cohortDefinition(getCohortDefinition(cohort))
                .cohortName(cohort.name)
                .description(cohort.description)
                .cdrVersionId(cdrVersionId!!)
                .matchedParticipantCount(cohortCount!!)
                .creationTime(Timestamp(clock.instant().toEpochMilli()))
                .lastModifiedTime(Timestamp(clock.instant().toEpochMilli()))
                .reviewedCount(0L)
                .reviewSize(0L)
                .reviewStatusEnum(ReviewStatus.NONE)
                .creator(creator)
    }

    /**
     * Helper method that will populate all gender, race and ethnicity per the spcecified list of
     * [ParticipantCohortStatus].
     *
     * @param participantCohortStatuses
     */
    private fun lookupGenderRaceEthnicityValues(
            participantCohortStatuses: List<ParticipantCohortStatus>) {
        val criteriaList = cbCriteriaDao.findGenderRaceEthnicity()
        participantCohortStatuses.forEach { pcs ->
            pcs.race = getName(criteriaList, pcs.raceConceptId)
            pcs.gender = getName(criteriaList, pcs.genderConceptId)
            pcs.ethnicity = getName(criteriaList, pcs.ethnicityConceptId)
        }
    }

    private fun getName(criteriaList: List<CBCriteria>, conceptId: Long?): String? {
        val returnList = criteriaList.stream()
                .filter { c -> c.conceptId == conceptId!!.toString() }
                .collect<List<CBCriteria>, Any>(Collectors.toList())
        return if (returnList.isEmpty()) null else returnList[0].name
    }

    /**
     * Helper method that generates a list of concept ids per demo
     *
     * @param pageRequest
     */
    private fun convertGenderRaceEthnicityFilters(pageRequest: PageRequest) {
        val filters = pageRequest.filters.stream()
                .map<Any> { filter ->
                    if (GENDER_RACE_ETHNICITY_TYPES.contains(filter.getProperty().name())) {
                        val possibleConceptIds = HashMap<Long, String>()
                        val values = possibleConceptIds.entries.stream()
                                .filter { entry -> filter.getValues().contains(entry.value) }
                                .map { entry -> entry.key.toString() }
                                .collect<List<String>, Any>(Collectors.toList())
                        return@pageRequest.getFilters().stream()
                                .map Filter ()
                                .property(filter.getProperty())
                                .operator(filter.getOperator())
                                .values(values)
                    }
                    filter
                }
                .collect<List<Filter>, Any>(Collectors.toList())
        pageRequest.filters(filters)
    }

    /**
     * Helper method that converts sortOrder if gender, race or ethnicity.
     *
     * @param pageRequest
     */
    private fun convertGenderRaceEthnicitySortOrder(pageRequest: PageRequest) {
        val sortColumn = pageRequest.sortColumn
        val sortName = pageRequest.sortOrder!!.name()
        val sort = if (sortName.equals(Direction.ASC.toString(), ignoreCase = true))
            Sort(Direction.ASC, "name")
        else
            Sort(Direction.DESC, "name")
        if (GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
            val criteriaList = cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
                    DomainType.PERSON.toString(), sortColumn, 0L, sort)
            val concepts = HashMap<String, Map<Long, String>>()
            val demoList = criteriaList.stream()
                    .map<Any> { c ->
                        ConceptIdName()
                                .conceptId(Long(c.conceptId))
                                .conceptName(c.name)
                    }
                    .sorted(Comparator.comparing(Function<Any, Any> { ConceptIdName.getConceptName() }))
                    .map<Any> { c -> c.getConceptId().toString() }
                    .collect<List<String>, Any>(Collectors.toList<Any>())
            if (!demoList.isEmpty()) {
                pageRequest.sortColumn = ("FIELD("
                        + ParticipantCohortStatusDbInfo.getDbName(sortColumn)
                        + ","
                        + demoList.joinToString(",")
                        + ") "
                        + pageRequest.sortOrder!!.name())
            }
        }
    }

    private fun createPageRequest(request: PageFilterRequest): PageRequest {
        val col = if (request.getDomain() == null) FilterColumns.PARTICIPANTID else START_DATE
        val sortColumn = Optional.ofNullable(request.getSortColumn()).orElse(col).toString()
        val pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE).toInt()
        val pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE).toInt()
        val sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC)
        return PageRequest()
                .page(pageParam)
                .pageSize(pageSizeParam)
                .sortOrder(sortOrderParam)
                .sortColumn(sortColumn)
                .filters(
                        if (request.getFilters() == null) ArrayList<Filter>() else request.getFilters().getItems())
    }

    /**
     * Helper method to convert a collection of [FieldValue] to [ParticipantData].
     *
     * @param rm
     * @param row
     * @param domain
     */
    private fun convertRowToParticipantData(
            rm: Map<String, Int>, row: List<FieldValue>, domain: DomainType): ParticipantData {
        return if (!domain.equals(DomainType.SURVEY)) {
            ParticipantData()
                    .itemDate(bigQueryService.getDateTime(row, rm[START_DATE.toString()]))
                    .domain(bigQueryService.getString(row, rm[DOMAIN.toString()]))
                    .standardName(bigQueryService.getString(row, rm[STANDARD_NAME.toString()]))
                    .ageAtEvent(bigQueryService.getLong(row, rm[AGE_AT_EVENT.toString()])!!.toInt())
                    .standardConceptId(bigQueryService.getLong(row, rm[STANDARD_CONCEPT_ID.toString()]))
                    .sourceConceptId(bigQueryService.getLong(row, rm[SOURCE_CONCEPT_ID.toString()]))
                    .standardVocabulary(bigQueryService.getString(row, rm[STANDARD_VOCAB.toString()]))
                    .sourceVocabulary(bigQueryService.getString(row, rm[SOURCE_VOCAB.toString()]))
                    .sourceName(bigQueryService.getString(row, rm[SOURCE_NAME.toString()]))
                    .sourceCode(bigQueryService.getString(row, rm[SOURCE_CODE.toString()]))
                    .standardCode(bigQueryService.getString(row, rm[STANDARD_CODE.toString()]))
                    .value(bigQueryService.getString(row, rm[VAL_AS_NUMBER.toString()]))
                    .visitType(bigQueryService.getString(row, rm[VISIT_TYPE.toString()]))
                    .numMentions(bigQueryService.getString(row, rm[NUM_OF_MENTIONS.toString()]))
                    .firstMention(
                            if (row.get(rm[FIRST_MENTION.toString()]).isNull)
                                ""
                            else
                                bigQueryService.getDateTime(row, rm[FIRST_MENTION.toString()]))
                    .lastMention(
                            if (row.get(rm[LAST_MENTION.toString()]).isNull)
                                ""
                            else
                                bigQueryService.getDateTime(row, rm[LAST_MENTION.toString()]))
                    .unit(bigQueryService.getString(row, rm[UNIT.toString()]))
                    .dose(bigQueryService.getString(row, rm[DOSE.toString()]))
                    .strength(bigQueryService.getString(row, rm[STRENGTH.toString()]))
                    .route(bigQueryService.getString(row, rm[ROUTE.toString()]))
                    .refRange(bigQueryService.getString(row, rm[REF_RANGE.toString()]))
        } else {
            ParticipantData()
                    .itemDate(bigQueryService.getDateTime(row, rm[START_DATE.toString()]))
                    .survey(bigQueryService.getString(row, rm[SURVEY_NAME.toString()]))
                    .question(bigQueryService.getString(row, rm[QUESTION.toString()]))
                    .answer(bigQueryService.getString(row, rm[ANSWER.toString()]))
        }
    }

    private fun convertRowToChartData(
            rm: Map<String, Int>, row: List<FieldValue>): ParticipantChartData {
        return ParticipantChartData()
                .standardName(bigQueryService.getString(row, rm["standardName"]))
                .standardVocabulary(bigQueryService.getString(row, rm["standardVocabulary"]))
                .startDate(bigQueryService.getDate(row, rm["startDate"]))
                .ageAtEvent(bigQueryService.getLong(row, rm["ageAtEvent"])!!.toInt())
                .rank(bigQueryService.getLong(row, rm["rank"])!!.toInt())
    }

    companion object {

        val PAGE = 0
        val PAGE_SIZE = 25
        val MAX_REVIEW_SIZE = 10000
        val MIN_LIMIT = 1
        val MAX_LIMIT = 20
        val DEFAULT_LIMIT = 5
        val GENDER_RACE_ETHNICITY_TYPES: List<String> = ImmutableList.of(
                FilterColumns.ETHNICITY.name(), FilterColumns.GENDER.name(), FilterColumns.RACE.name())
        private val log = Logger.getLogger(CohortReviewController::class.java.name)

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_PARTICIPANT = { participant ->
            org.pmiops.workbench.model.ParticipantCohortStatus()
                    .participantId(participant.participantKey!!.participantId)
                    .status(participant.statusEnum)
                    .birthDate(participant.birthDate!!.toString())
                    .ethnicityConceptId(participant.ethnicityConceptId)
                    .ethnicity(participant.ethnicity)
                    .genderConceptId(participant.genderConceptId)
                    .gender(participant.gender)
                    .raceConceptId(participant.raceConceptId)
                    .race(participant.race)
                    .deceased(participant.deceased)
        }

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_COHORTREVIEW_WITH_PAGING = { cohortReview, pageRequest ->
            org.pmiops.workbench.model.CohortReview()
                    .cohortReviewId(cohortReview.cohortReviewId)
                    .cohortId(cohortReview.cohortId)
                    .cdrVersionId(cohortReview.cdrVersionId)
                    .creationTime(cohortReview.creationTime!!.toString())
                    .cohortDefinition(cohortReview.cohortDefinition)
                    .cohortName(cohortReview.cohortName)
                    .description(cohortReview.description)
                    .matchedParticipantCount(cohortReview.matchedParticipantCount)
                    .reviewedCount(cohortReview.reviewedCount)
                    .reviewStatus(cohortReview.reviewStatusEnum)
                    .reviewSize(cohortReview.reviewSize)
                    .page(pageRequest.page)
                    .pageSize(pageRequest.pageSize)
                    .sortOrder(pageRequest.sortOrder!!.toString())
                    .sortColumn(pageRequest.sortColumn)
        }

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_COHORTREVIEW = { cohortReview ->
            org.pmiops.workbench.model.CohortReview()
                    .cohortReviewId(cohortReview.cohortReviewId)
                    .etag(Etags.fromVersion(cohortReview.version))
                    .cohortId(cohortReview.cohortId)
                    .cdrVersionId(cohortReview.cdrVersionId)
                    .creationTime(cohortReview.creationTime!!.toString())
                    .lastModifiedTime(cohortReview.lastModifiedTime!!.getTime())
                    .cohortDefinition(cohortReview.cohortDefinition)
                    .cohortName(cohortReview.cohortName)
                    .description(cohortReview.description)
                    .matchedParticipantCount(cohortReview.matchedParticipantCount)
                    .reviewedCount(cohortReview.reviewedCount)
                    .reviewStatus(cohortReview.reviewStatusEnum)
                    .reviewSize(cohortReview.reviewSize)
        }

        private val FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION = { participantCohortAnnotation ->
            org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
                    .annotationId(participantCohortAnnotation.getAnnotationId())
                    .cohortAnnotationDefinitionId(
                            participantCohortAnnotation.getCohortAnnotationDefinitionId())
                    .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
                    .participantId(participantCohortAnnotation.getParticipantId())
                    .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
                    .annotationValueEnum(participantCohortAnnotation.getAnnotationValueEnum())
                    .annotationValueDateString(participantCohortAnnotation.getAnnotationValueDate())
                    .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
                    .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger())
        }

        private val TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION = { participantCohortAnnotation ->
            val date = if (participantCohortAnnotation.annotationValueDate == null)
                null
            else
                participantCohortAnnotation.annotationValueDate!!.toString()
            val enumValue = if (participantCohortAnnotation.cohortAnnotationEnumValue == null)
                null
            else
                participantCohortAnnotation.cohortAnnotationEnumValue!!.name
            ParticipantCohortAnnotation()
                    .annotationId(participantCohortAnnotation.annotationId)
                    .cohortAnnotationDefinitionId(
                            participantCohortAnnotation.cohortAnnotationDefinitionId)
                    .cohortReviewId(participantCohortAnnotation.cohortReviewId)
                    .participantId(participantCohortAnnotation.participantId)
                    .annotationValueString(participantCohortAnnotation.annotationValueString)
                    .annotationValueEnum(enumValue)
                    .annotationValueDate(date)
                    .annotationValueBoolean(participantCohortAnnotation.annotationValueBoolean)
                    .annotationValueInteger(participantCohortAnnotation.annotationValueInteger)
        }
    }
}
