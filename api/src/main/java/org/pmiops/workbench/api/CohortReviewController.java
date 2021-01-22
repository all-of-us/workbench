package org.pmiops.workbench.api;

import static org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT;
import static org.pmiops.workbench.model.FilterColumns.ANSWER;
import static org.pmiops.workbench.model.FilterColumns.DOMAIN;
import static org.pmiops.workbench.model.FilterColumns.DOSE;
import static org.pmiops.workbench.model.FilterColumns.FIRST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.LAST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.NUM_MENTIONS;
import static org.pmiops.workbench.model.FilterColumns.QUESTION;
import static org.pmiops.workbench.model.FilterColumns.REF_RANGE;
import static org.pmiops.workbench.model.FilterColumns.ROUTE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CODE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_NAME;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_VOCABULARY;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CODE;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_NAME;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_VOCABULARY;
import static org.pmiops.workbench.model.FilterColumns.START_DATETIME;
import static org.pmiops.workbench.model.FilterColumns.STRENGTH;
import static org.pmiops.workbench.model.FilterColumns.SURVEY_NAME;
import static org.pmiops.workbench.model.FilterColumns.UNIT;
import static org.pmiops.workbench.model.FilterColumns.VALUE_AS_NUMBER;
import static org.pmiops.workbench.model.FilterColumns.VISIT_TYPE;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Provider;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CohortReviewWithCountResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataCountResponse;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

  public static final Integer PAGE = 0;
  public static final Integer PAGE_SIZE = 25;
  public static final Integer MAX_REVIEW_SIZE = 10000;
  public static final Integer MIN_LIMIT = 1;
  public static final Integer MAX_LIMIT = 20;
  public static final Integer DEFAULT_LIMIT = 5;
  public static final List<String> SEX_GENDER_RACE_ETHNICITY_TYPES =
      ImmutableList.of(
          FilterColumns.SEXATBIRTH.name(),
          FilterColumns.ETHNICITY.name(),
          FilterColumns.GENDER.name(),
          FilterColumns.RACE.name());

  private final CohortBuilderService cohortBuilderService;
  private final CohortReviewService cohortReviewService;
  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final ReviewQueryBuilder reviewQueryBuilder;
  private final UserRecentResourceService userRecentResourceService;
  private final Provider<DbUser> userProvider;
  private final WorkspaceService workspaceService;
  private final Clock clock;

  @Autowired
  CohortReviewController(
      CohortReviewService cohortReviewService,
      BigQueryService bigQueryService,
      CohortBuilderService cohortBuilderService,
      CohortQueryBuilder cohortQueryBuilder,
      ReviewQueryBuilder reviewQueryBuilder,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider,
      WorkspaceService workspaceService,
      Clock clock) {
    this.cohortReviewService = cohortReviewService;
    this.bigQueryService = bigQueryService;
    this.cohortBuilderService = cohortBuilderService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.reviewQueryBuilder = reviewQueryBuilder;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
    this.clock = clock;
  }

  /**
   * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If
   * participant cohort status data exists for a review or no cohort review exists for
   * cohortReviewId then throw a {@link BadRequestException}.
   */
  @Override
  public ResponseEntity<CohortReview> createCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long cdrVersionId,
      CreateReviewRequest request) {
    if (request.getSize() <= 0 || request.getSize() > MAX_REVIEW_SIZE) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE));
    }

    // this validates that the user is in the proper workspace
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    CohortReview cohortReview;
    DbCohort cohort = cohortReviewService.findCohort(cohortId);
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort);
      cohortReview = cohortReviewService.saveCohortReview(cohortReview, userProvider.get());
    }
    if (cohortReview.getReviewSize() > 0) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
              cohortId, cdrVersionId));
    }

    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildRandomParticipantQuery(
                    new ParticipantCriteria(searchRequest), request.getSize(), 0L)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<DbParticipantCohortStatus> participantCohortStatuses =
        createParticipantCohortStatusesList(cohortReview.getCohortReviewId(), result, rm);

    cohortReview
        .reviewSize(Long.valueOf(participantCohortStatuses.size()))
        .reviewStatus(ReviewStatus.CREATED);

    // when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
    cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses);

    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());

    List<ParticipantCohortStatus> paginatedPCS =
        cohortReviewService.findAll(cohortReview.getCohortReviewId(), pageRequest);

    return ResponseEntity.ok(cohortReview.participantCohortStatuses(paginatedPCS));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      ParticipantCohortAnnotation request) {

    if (!request.getCohortReviewId().equals(cohortReviewId)) {
      throw new BadRequestException(
          "Bad Request: request cohort review id must equal path parameter cohort review id.");
    }

    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        cohortReviewService.saveParticipantCohortAnnotation(request.getCohortReviewId(), request));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohortReview(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    cohortReviewService.deleteCohortReview(cohortReviewId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId) {

    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    // will throw a NotFoundException if participant cohort annotation does not exist
    cohortReviewService.deleteParticipantCohortAnnotation(
        annotationId, cohortReviewId, participantId);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<CohortChartDataListResponse> getCohortChartData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      String domain,
      Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a chart limit between %d and %d.",
              MIN_LIMIT, MAX_LIMIT));
    }

    CohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest),
                    Objects.requireNonNull(Domain.fromValue(domain)),
                    chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    CohortChartDataListResponse response = new CohortChartDataListResponse();
    response.count(cohortReview.getMatchedParticipantCount());
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(
          new CohortChartData()
              .name(bigQueryService.getString(row, rm.get("name")))
              .conceptId(bigQueryService.getLong(row, rm.get("conceptId")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new CohortReviewListResponse()
            .items(
                cohortReviewService.getRequiredWithCohortReviews(workspaceNamespace, workspaceId)));
  }

  @Override
  public ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      String domain,
      Integer limit) {
    int chartLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    if (chartLimit < MIN_LIMIT || chartLimit > MAX_LIMIT) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a chart limit between %d and %d.",
              MIN_LIMIT, MAX_LIMIT));
    }
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildChartDataQuery(
                    participantId, Objects.requireNonNull(Domain.fromValue(domain)), chartLimit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantChartDataListResponse response = new ParticipantChartDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToChartData(rm, row));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(
      String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<ParticipantCohortAnnotation> annotations =
        cohortReviewService.findParticipantCohortAnnotations(cohortReviewId, participantId);

    return ResponseEntity.ok(new ParticipantCohortAnnotationListResponse().items(annotations));
  }

  @Override
  public ResponseEntity<ParticipantCohortStatus> getParticipantCohortStatus(
      String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId));
  }

  /**
   * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
   * based on page, pageSize, sortOrder and sortColumn.
   */
  @Override
  public ResponseEntity<CohortReviewWithCountResponse> getParticipantCohortStatuses(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long cdrVersionId,
      PageFilterRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    CohortReview cohortReview;
    List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    DbCohort cohort = cohortReviewService.findCohort(cohortId);
    PageRequest pageRequest = createPageRequest(request);
    convertGenderRaceEthnicitySortOrder(pageRequest);

    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
      participantCohortStatuses =
          cohortReviewService.findAll(cohortReview.getCohortReviewId(), pageRequest);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort);
    }

    cohortReview.participantCohortStatuses(participantCohortStatuses);

    userRecentResourceService.updateCohortEntry(
        cohort.getWorkspaceId(), userProvider.get().getUserId(), cohortId);
    return ResponseEntity.ok(
        new CohortReviewWithCountResponse()
            .cohortReview(cohortReview)
            .queryResultSize(
                pageRequest.getFilters().isEmpty()
                    ? cohortReview.getReviewSize()
                    : cohortReviewService.findCount(
                        cohortReview.getCohortReviewId(), pageRequest)));
  }

  @Override
  public ResponseEntity<ParticipantDataCountResponse> getParticipantCount(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      PageFilterRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    // get the total records count for this participant per specified domain
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildCountQuery(
                    participantId, request.getDomain(), createPageRequest(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    ParticipantDataCountResponse response =
        new ParticipantDataCountResponse()
            .count(bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ParticipantDataListResponse> getParticipantData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      PageFilterRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    // this validates that the participant is in the requested review.
    cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildQuery(
                    participantId, request.getDomain(), createPageRequest(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponse response = new ParticipantDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantData(rm, row, request.getDomain()));
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<VocabularyListResponse> getVocabularies(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(reviewQueryBuilder.buildVocabularyDataQuery()));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    VocabularyListResponse response = new VocabularyListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(
          new Vocabulary()
              .domain(bigQueryService.getString(row, rm.get("domain")))
              .type(bigQueryService.getString(row, rm.get("type")))
              .vocabulary(bigQueryService.getString(row, rm.get("vocabulary"))));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CohortReview> updateCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      CohortReview cohortReview) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        cohortReviewService.updateCohortReview(
            cohortReview, cohortReviewId, new Timestamp(clock.instant().toEpochMilli())));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId,
      ModifyParticipantCohortAnnotationRequest request) {
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        cohortReviewService.updateParticipantCohortAnnotation(
            annotationId, cohortReviewId, participantId, request));
  }

  @Override
  public ResponseEntity<ParticipantCohortStatus> updateParticipantCohortStatus(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      ModifyCohortStatusRequest cohortStatusRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        cohortReviewService.updateParticipantCohortStatus(
            cohortReviewId,
            participantId,
            cohortStatusRequest.getStatus(),
            new Timestamp(clock.instant().toEpochMilli())));
  }

  /** Helper method to create a new {@link CohortReview}. */
  private CohortReview initializeCohortReview(Long cdrVersionId, DbCohort cohort) {
    SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    long cohortCount = bigQueryService.getLong(row, rm.get("count"));

    return createNewCohortReview(cohort, cdrVersionId, cohortCount);
  }

  /**
   * Helper method that builds a list of {@link DbParticipantCohortStatus} from BigQuery results.
   */
  private List<DbParticipantCohortStatus> createParticipantCohortStatusesList(
      Long cohortReviewId, TableResult result, Map<String, Integer> rm) {
    List<DbParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      String birthDateTimeString = bigQueryService.getString(row, rm.get("birth_datetime"));
      if (birthDateTimeString == null) {
        throw new BigQueryException(
            500, "birth_datetime is null at position: " + rm.get("birth_datetime"));
      }
      java.util.Date birthDate =
          Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000));
      participantCohortStatuses.add(
          new DbParticipantCohortStatus()
              .participantKey(
                  new DbParticipantCohortStatusKey(
                      cohortReviewId, bigQueryService.getLong(row, rm.get("person_id"))))
              .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
              .birthDate(new Date(birthDate.getTime()))
              .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
              .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
              .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id")))
              .sexAtBirthConceptId(bigQueryService.getLong(row, rm.get("sex_at_birth_concept_id")))
              .deceased(bigQueryService.getBoolean(row, rm.get("deceased"))));
    }
    return participantCohortStatuses;
  }

  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a {@link
   * NotFoundException} if {@link DbCohort#getCriteria()} return null.
   */
  private String getCohortDefinition(DbCohort cohort) {
    String definition = cohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
    }
    return definition;
  }

  /** Helper method that constructs a {@link CohortReview} with the specified ids and count. */
  private CohortReview createNewCohortReview(DbCohort cohort, Long cdrVersionId, Long cohortCount) {
    return new CohortReview()
        .cohortId(cohort.getCohortId())
        .cohortDefinition(getCohortDefinition(cohort))
        .cohortName(cohort.getName())
        .description(cohort.getDescription())
        .cdrVersionId(cdrVersionId)
        .matchedParticipantCount(cohortCount)
        .creationTime(new Timestamp(clock.instant().toEpochMilli()).getTime())
        .lastModifiedTime(new Timestamp(clock.instant().toEpochMilli()).getTime())
        .reviewedCount(0L)
        .reviewSize(0L)
        .reviewStatus(ReviewStatus.NONE);
  }

  /** Helper method that converts sortOrder if gender, race or ethnicity. */
  private void convertGenderRaceEthnicitySortOrder(PageRequest pageRequest) {
    String sortColumn = pageRequest.getSortColumn();
    String sortName = pageRequest.getSortOrder().name();
    if (SEX_GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
      String criteriaSortColumn =
          sortColumn.equals(FilterColumns.SEXATBIRTH.name())
              ? CriteriaType.SEX.toString()
              : sortColumn;
      List<String> demoList =
          cohortBuilderService.findSortedConceptIdsByDomainIdAndType(
              Domain.PERSON.toString(), criteriaSortColumn, sortName);
      if (!demoList.isEmpty()) {
        pageRequest.setSortColumn(
            "FIELD("
                + ParticipantCohortStatusDbInfo.getDbName(sortColumn)
                + ","
                + String.join(",", demoList)
                + ") "
                + pageRequest.getSortOrder().name());
      }
    }
  }

  private PageRequest createPageRequest(PageFilterRequest request) {
    FilterColumns col = request.getDomain() == null ? FilterColumns.PARTICIPANTID : START_DATETIME;
    String sortColumn = Optional.ofNullable(request.getSortColumn()).orElse(col).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam =
        Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest()
        .page(pageParam)
        .pageSize(pageSizeParam)
        .sortOrder(sortOrderParam)
        .sortColumn(sortColumn)
        .filters(
            request.getFilters() == null ? new ArrayList<>() : request.getFilters().getItems());
  }

  /** Helper method to convert a collection of {@link FieldValue} to {@link ParticipantData}. */
  private ParticipantData convertRowToParticipantData(
      Map<String, Integer> rm, List<FieldValue> row, Domain domain) {
    if (!domain.equals(Domain.SURVEY)) {
      return new ParticipantData()
          .itemDate(bigQueryService.getDateTime(row, rm.get(START_DATETIME.toString())))
          .domain(bigQueryService.getString(row, rm.get(DOMAIN.toString())))
          .standardName(bigQueryService.getString(row, rm.get(STANDARD_NAME.toString())))
          .ageAtEvent(bigQueryService.getLong(row, rm.get(AGE_AT_EVENT.toString())).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get(STANDARD_CONCEPT_ID.toString())))
          .sourceConceptId(bigQueryService.getLong(row, rm.get(SOURCE_CONCEPT_ID.toString())))
          .standardVocabulary(
              bigQueryService.getString(row, rm.get(STANDARD_VOCABULARY.toString())))
          .sourceVocabulary(bigQueryService.getString(row, rm.get(SOURCE_VOCABULARY.toString())))
          .sourceName(bigQueryService.getString(row, rm.get(SOURCE_NAME.toString())))
          .sourceCode(bigQueryService.getString(row, rm.get(SOURCE_CODE.toString())))
          .standardCode(bigQueryService.getString(row, rm.get(STANDARD_CODE.toString())))
          .value(bigQueryService.getString(row, rm.get(VALUE_AS_NUMBER.toString())))
          .visitType(bigQueryService.getString(row, rm.get(VISIT_TYPE.toString())))
          .numMentions(bigQueryService.getString(row, rm.get(NUM_MENTIONS.toString())))
          .firstMention(
              row.get(rm.get(FIRST_MENTION.toString())).isNull()
                  ? ""
                  : bigQueryService.getDateTime(row, rm.get(FIRST_MENTION.toString())))
          .lastMention(
              row.get(rm.get(LAST_MENTION.toString())).isNull()
                  ? ""
                  : bigQueryService.getDateTime(row, rm.get(LAST_MENTION.toString())))
          .unit(bigQueryService.getString(row, rm.get(UNIT.toString())))
          .dose(bigQueryService.getString(row, rm.get(DOSE.toString())))
          .strength(bigQueryService.getString(row, rm.get(STRENGTH.toString())))
          .route(bigQueryService.getString(row, rm.get(ROUTE.toString())))
          .refRange(bigQueryService.getString(row, rm.get(REF_RANGE.toString())));
    } else {
      return new ParticipantData()
          .itemDate(bigQueryService.getDateTime(row, rm.get(START_DATETIME.toString())))
          .survey(bigQueryService.getString(row, rm.get(SURVEY_NAME.toString())))
          .question(bigQueryService.getString(row, rm.get(QUESTION.toString())))
          .answer(bigQueryService.getString(row, rm.get(ANSWER.toString())));
    }
  }

  private ParticipantChartData convertRowToChartData(
      Map<String, Integer> rm, List<FieldValue> row) {
    return new ParticipantChartData()
        .standardName(bigQueryService.getString(row, rm.get("standardName")))
        .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
        .startDate(bigQueryService.getDate(row, rm.get("startDate")))
        .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
        .rank(bigQueryService.getLong(row, rm.get("rank")).intValue());
  }
}
