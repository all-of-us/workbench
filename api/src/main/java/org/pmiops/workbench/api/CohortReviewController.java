package org.pmiops.workbench.api;

import static org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT;
import static org.pmiops.workbench.model.FilterColumns.ANSWER;
import static org.pmiops.workbench.model.FilterColumns.DOMAIN;
import static org.pmiops.workbench.model.FilterColumns.DOSE;
import static org.pmiops.workbench.model.FilterColumns.FIRST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.LAST_MENTION;
import static org.pmiops.workbench.model.FilterColumns.NUM_OF_MENTIONS;
import static org.pmiops.workbench.model.FilterColumns.QUESTION;
import static org.pmiops.workbench.model.FilterColumns.REF_RANGE;
import static org.pmiops.workbench.model.FilterColumns.ROUTE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CODE;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_NAME;
import static org.pmiops.workbench.model.FilterColumns.SOURCE_VOCAB;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CODE;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_NAME;
import static org.pmiops.workbench.model.FilterColumns.STANDARD_VOCAB;
import static org.pmiops.workbench.model.FilterColumns.START_DATE;
import static org.pmiops.workbench.model.FilterColumns.STRENGTH;
import static org.pmiops.workbench.model.FilterColumns.SURVEY_NAME;
import static org.pmiops.workbench.model.FilterColumns.UNIT;
import static org.pmiops.workbench.model.FilterColumns.VAL_AS_NUMBER;
import static org.pmiops.workbench.model.FilterColumns.VISIT_TYPE;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
  public static final List<String> GENDER_RACE_ETHNICITY_TYPES =
      ImmutableList.of(
          FilterColumns.ETHNICITY.name(), FilterColumns.GENDER.name(), FilterColumns.RACE.name());

  private CBCriteriaDao cbCriteriaDao;
  private CohortReviewService cohortReviewService;
  private BigQueryService bigQueryService;
  private CohortQueryBuilder cohortQueryBuilder;
  private ReviewQueryBuilder reviewQueryBuilder;
  private UserRecentResourceService userRecentResourceService;
  private Provider<DbUser> userProvider;
  private final Clock clock;
  private static final Logger log = Logger.getLogger(CohortReviewController.class.getName());

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final Function<
      DbParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>
      TO_CLIENT_PARTICIPANT =
          participant ->
              new org.pmiops.workbench.model.ParticipantCohortStatus()
                  .participantId(participant.getParticipantKey().getParticipantId())
                  .status(participant.getStatusEnum())
                  .birthDate(participant.getBirthDate().toString())
                  .ethnicityConceptId(participant.getEthnicityConceptId())
                  .ethnicity(participant.getEthnicity())
                  .genderConceptId(participant.getGenderConceptId())
                  .gender(participant.getGender())
                  .raceConceptId(participant.getRaceConceptId())
                  .race(participant.getRace())
                  .deceased(participant.getDeceased());

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final BiFunction<
      DbCohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>
      TO_CLIENT_COHORTREVIEW_WITH_PAGING =
          (cohortReview, pageRequest) ->
              new org.pmiops.workbench.model.CohortReview()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .cohortId(cohortReview.getCohortId())
                  .cdrVersionId(cohortReview.getCdrVersionId())
                  .creationTime(cohortReview.getCreationTime().toString())
                  .cohortDefinition(cohortReview.getCohortDefinition())
                  .cohortName(cohortReview.getCohortName())
                  .description(cohortReview.getDescription())
                  .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                  .reviewedCount(cohortReview.getReviewedCount())
                  .reviewStatus(cohortReview.getReviewStatusEnum())
                  .reviewSize(cohortReview.getReviewSize())
                  .page(pageRequest.getPage())
                  .pageSize(pageRequest.getPageSize())
                  .sortOrder(pageRequest.getSortOrder().toString())
                  .sortColumn(pageRequest.getSortColumn());

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final Function<DbCohortReview, org.pmiops.workbench.model.CohortReview>
      TO_CLIENT_COHORTREVIEW =
          cohortReview ->
              new org.pmiops.workbench.model.CohortReview()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .etag(Etags.fromVersion(cohortReview.getVersion()))
                  .cohortId(cohortReview.getCohortId())
                  .cdrVersionId(cohortReview.getCdrVersionId())
                  .creationTime(cohortReview.getCreationTime().toString())
                  .lastModifiedTime(cohortReview.getLastModifiedTime().getTime())
                  .cohortDefinition(cohortReview.getCohortDefinition())
                  .cohortName(cohortReview.getCohortName())
                  .description(cohortReview.getDescription())
                  .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                  .reviewedCount(cohortReview.getReviewedCount())
                  .reviewStatus(cohortReview.getReviewStatusEnum())
                  .reviewSize(cohortReview.getReviewSize());

  private static final Function<
          ParticipantCohortAnnotation, DbParticipantCohortAnnotation>
      FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
          participantCohortAnnotation ->
              new DbParticipantCohortAnnotation()
                  .annotationId(participantCohortAnnotation.getAnnotationId())
                  .cohortAnnotationDefinitionId(
                      participantCohortAnnotation.getCohortAnnotationDefinitionId())
                  .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
                  .participantId(participantCohortAnnotation.getParticipantId())
                  .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
                  .annotationValueEnum(participantCohortAnnotation.getAnnotationValueEnum())
                  .annotationValueDateString(participantCohortAnnotation.getAnnotationValueDate())
                  .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
                  .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger());

  private static final Function<
      DbParticipantCohortAnnotation, ParticipantCohortAnnotation>
      TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION =
          participantCohortAnnotation -> {
            String date =
                participantCohortAnnotation.getAnnotationValueDate() == null
                    ? null
                    : participantCohortAnnotation.getAnnotationValueDate().toString();
            String enumValue =
                participantCohortAnnotation.getCohortAnnotationEnumValue() == null
                    ? null
                    : participantCohortAnnotation.getCohortAnnotationEnumValue().getName();
            return new ParticipantCohortAnnotation()
                .annotationId(participantCohortAnnotation.getAnnotationId())
                .cohortAnnotationDefinitionId(
                    participantCohortAnnotation.getCohortAnnotationDefinitionId())
                .cohortReviewId(participantCohortAnnotation.getCohortReviewId())
                .participantId(participantCohortAnnotation.getParticipantId())
                .annotationValueString(participantCohortAnnotation.getAnnotationValueString())
                .annotationValueEnum(enumValue)
                .annotationValueDate(date)
                .annotationValueBoolean(participantCohortAnnotation.getAnnotationValueBoolean())
                .annotationValueInteger(participantCohortAnnotation.getAnnotationValueInteger());
          };

  @Autowired
  CohortReviewController(
      CBCriteriaDao cbCriteriaDao,
      CohortReviewService cohortReviewService,
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      ReviewQueryBuilder reviewQueryBuilder,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider,
      Clock clock) {
    this.cbCriteriaDao = cbCriteriaDao;
    this.cohortReviewService = cohortReviewService;
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.reviewQueryBuilder = reviewQueryBuilder;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<DbUser> userProvider) {
    this.userProvider = userProvider;
  }

  /**
   * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If
   * participant cohort status data exists for a review or no cohort review exists for
   * cohortReviewId then throw a {@link BadRequestException}.
   *
   * @param workspaceNamespace
   * @param workspaceId
   * @param cohortId
   * @param cdrVersionId
   * @param request
   */
  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> createCohortReview(
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

    DbCohort cohort = cohortReviewService.findCohort(cohortId);
    // this validates that the user is in the proper workspace
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);
    DbCohortReview cohortReview = null;
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider.get());
      cohortReviewService.saveCohortReview(cohortReview);
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
        .reviewSize(participantCohortStatuses.size())
        .reviewStatusEnum(ReviewStatus.CREATED);

    // when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
    cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses);

    PageRequest pageRequest =
        new PageRequest()
            .page(PAGE)
            .pageSize(PAGE_SIZE)
            .sortOrder(SortOrder.ASC)
            .sortColumn(FilterColumns.PARTICIPANTID.toString());

    List<DbParticipantCohortStatus> paginatedPCS =
        cohortReviewService.findAll(cohortReview.getCohortReviewId(), pageRequest);
    lookupGenderRaceEthnicityValues(paginatedPCS);

    org.pmiops.workbench.model.CohortReview responseReview =
        TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
        paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    return ResponseEntity.ok(responseReview);
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

    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbParticipantCohortAnnotation participantCohortAnnotation =
        FROM_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(request);

    participantCohortAnnotation =
        cohortReviewService.saveParticipantCohortAnnotation(
            request.getCohortReviewId(), participantCohortAnnotation);

    return ResponseEntity.ok(
        TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohortReview(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbCohortReview dbCohortReview =
        cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId);
    cohortReviewService.deleteCohortReview(dbCohortReview);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId) {

    cohortReviewService.enforceWorkspaceAccessLevel(
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

    DbCohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest),
                    DomainType.fromValue(domain),
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
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<DbCohortReview> reviews =
        cohortReviewService.getRequiredWithCohortReviews(workspaceNamespace, workspaceId);
    CohortReviewListResponse response = new CohortReviewListResponse();
    response.setItems(reviews.stream().map(TO_CLIENT_COHORTREVIEW).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
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
    DbCohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildChartDataQuery(
                    participantId, DomainType.fromValue(domain), chartLimit)));
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
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<DbParticipantCohortAnnotation> annotations =
        cohortReviewService.findParticipantCohortAnnotations(cohortReviewId, participantId);

    ParticipantCohortAnnotationListResponse response =
        new ParticipantCohortAnnotationListResponse();
    response.setItems(
        annotations.stream()
            .map(TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION)
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
      getParticipantCohortStatus(
          String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    DbCohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    DbParticipantCohortStatus status =
        cohortReviewService.findParticipantCohortStatus(review.getCohortReviewId(), participantId);
    lookupGenderRaceEthnicityValues(Arrays.asList(status));
    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(status));
  }

  /**
   * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
   * based on page, pageSize, sortOrder and sortColumn.
   */
  @Override
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> getParticipantCohortStatuses(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long cdrVersionId,
      PageFilterRequest request) {
    DbCohortReview cohortReview = null;
    DbCohort cohort = cohortReviewService.findCohort(cohortId);

    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
    try {
      cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
    } catch (NotFoundException nfe) {
      cohortReview = initializeCohortReview(cdrVersionId, cohort, userProvider.get());
    }

    PageRequest pageRequest = createPageRequest(request);
    convertGenderRaceEthnicityFilters(pageRequest);
    convertGenderRaceEthnicitySortOrder(pageRequest);

    List<DbParticipantCohortStatus> participantCohortStatuses =
        cohortReviewService.findAll(cohortReview.getCohortReviewId(), pageRequest);
    lookupGenderRaceEthnicityValues(participantCohortStatuses);

    Long queryResultSize =
        pageRequest.getFilters().isEmpty()
            ? cohortReview.getReviewSize()
            : cohortReviewService.findCount(cohortReview.getCohortReviewId(), pageRequest);

    org.pmiops.workbench.model.CohortReview responseReview =
        TO_CLIENT_COHORTREVIEW_WITH_PAGING.apply(cohortReview, pageRequest);
    responseReview.setParticipantCohortStatuses(
        participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));
    responseReview.setQueryResultSize(queryResultSize);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    userRecentResourceService.updateCohortEntry(
        cohort.getWorkspaceId(), userProvider.get().getUserId(), cohortId, now);
    return ResponseEntity.ok(responseReview);
  }

  @Override
  public ResponseEntity<ParticipantDataListResponse> getParticipantData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      PageFilterRequest request) {
    DbCohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

    // this validates that the participant is in the requested review.
    cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

    DomainType domain = request.getDomain();
    PageRequest pageRequest = createPageRequest(request);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    ParticipantDataListResponse response = new ParticipantDataListResponse();
    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(convertRowToParticipantData(rm, row, domain));
    }

    if (result.getTotalRows() == pageRequest.getPageSize()) {
      result =
          bigQueryService.executeQuery(
              bigQueryService.filterBigQueryConfig(
                  reviewQueryBuilder.buildCountQuery(participantId, domain, pageRequest)));
      rm = bigQueryService.getResultMapper(result);
      response.count(
          bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count")));
    } else {
      response.count(result.getTotalRows());
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<VocabularyListResponse> getVocabularies(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    DbCohortReview review = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(review.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);

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
  public ResponseEntity<org.pmiops.workbench.model.CohortReview> updateCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      org.pmiops.workbench.model.CohortReview cohortReview) {
    // This also enforces registered auth domain.
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    DbCohortReview dbCohortReview =
        cohortReviewService.findCohortReview(workspaceNamespace, workspaceId, cohortReviewId);
    if (Strings.isNullOrEmpty(cohortReview.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(cohortReview.getEtag());
    if (dbCohortReview.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated cohort review version");
    }
    if (cohortReview.getCohortName() != null) {
      dbCohortReview.setCohortName(cohortReview.getCohortName());
    }
    if (cohortReview.getDescription() != null) {
      dbCohortReview.setDescription(cohortReview.getDescription());
    }
    dbCohortReview.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    try {
      cohortReviewService.saveCohortReview(dbCohortReview);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort review update", e);
      throw new ConflictException("Failed due to concurrent cohort review modification");
    }
    return ResponseEntity.ok(TO_CLIENT_COHORTREVIEW.apply(dbCohortReview));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId,
      ModifyParticipantCohortAnnotationRequest request) {
    cohortReviewService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbParticipantCohortAnnotation participantCohortAnnotation =
        cohortReviewService.updateParticipantCohortAnnotation(
            annotationId, cohortReviewId, participantId, request);

    return ResponseEntity.ok(
        TO_CLIENT_PARTICIPANT_COHORT_ANNOTATION.apply(participantCohortAnnotation));
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus>
      updateParticipantCohortStatus(
          String workspaceNamespace,
          String workspaceId,
          Long cohortReviewId,
          Long participantId,
          ModifyCohortStatusRequest cohortStatusRequest) {
    DbCohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort cohort = cohortReviewService.findCohort(cohortReview.getCohortId());
    cohortReviewService.validateMatchingWorkspaceAndSetCdrVersion(
        workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);

    DbParticipantCohortStatus participantCohortStatus =
        cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

    participantCohortStatus.setStatusEnum(cohortStatusRequest.getStatus());
    cohortReviewService.saveParticipantCohortStatus(participantCohortStatus);
    lookupGenderRaceEthnicityValues(Arrays.asList(participantCohortStatus));

    cohortReview.lastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    cohortReview.incrementReviewedCount();
    cohortReviewService.saveCohortReview(cohortReview);

    return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
  }

  /**
   * Helper method to create a new {@link DbCohortReview}.
   *
   * @param cdrVersionId
   * @param cohort
   * @param creator
   */
  private DbCohortReview initializeCohortReview(Long cdrVersionId, DbCohort cohort, DbUser creator) {
    SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    long cohortCount = bigQueryService.getLong(row, rm.get("count"));

    return createNewCohortReview(cohort, cdrVersionId, cohortCount, creator);
  }

  /**
   * Helper method that builds a list of {@link DbParticipantCohortStatus} from BigQuery results.
   *
   * @param cohortReviewId
   * @param result
   * @param rm
   * @return
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
              .statusEnum(CohortStatus.NOT_REVIEWED)
              .birthDate(new Date(birthDate.getTime()))
              .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
              .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
              .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id")))
              .deceased(bigQueryService.getBoolean(row, rm.get("deceased"))));
    }
    return participantCohortStatuses;
  }

  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a {@link
   * NotFoundException} if {@link DbCohort#getCriteria()} return null.
   *
   * @param cohort
   * @return
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

  /**
   * Helper method that constructs a {@link DbCohortReview} with the specified ids and count.
   *
   * @param cohort
   * @param cdrVersionId
   * @param cohortCount
   * @param creator
   * @return
   */
  private DbCohortReview createNewCohortReview(
      DbCohort cohort, Long cdrVersionId, Long cohortCount, DbUser creator) {
    return new DbCohortReview()
        .cohortId(cohort.getCohortId())
        .cohortDefinition(getCohortDefinition(cohort))
        .cohortName(cohort.getName())
        .description(cohort.getDescription())
        .cdrVersionId(cdrVersionId)
        .matchedParticipantCount(cohortCount)
        .creationTime(new Timestamp(clock.instant().toEpochMilli()))
        .lastModifiedTime(new Timestamp(clock.instant().toEpochMilli()))
        .reviewedCount(0L)
        .reviewSize(0L)
        .reviewStatusEnum(ReviewStatus.NONE)
        .creator(creator);
  }

  /**
   * Helper method that will populate all gender, race and ethnicity per the spcecified list of
   * {@link DbParticipantCohortStatus}.
   *
   * @param participantCohortStatuses
   */
  private void lookupGenderRaceEthnicityValues(
      List<DbParticipantCohortStatus> participantCohortStatuses) {
    List<CBCriteria> criteriaList = cbCriteriaDao.findGenderRaceEthnicity();
    participantCohortStatuses.forEach(
        pcs -> {
          pcs.setRace(getName(criteriaList, pcs.getRaceConceptId()));
          pcs.setGender(getName(criteriaList, pcs.getGenderConceptId()));
          pcs.setEthnicity(getName(criteriaList, pcs.getEthnicityConceptId()));
        });
  }

  private String getName(List<CBCriteria> criteriaList, Long conceptId) {
    List<CBCriteria> returnList =
        criteriaList.stream()
            .filter(c -> c.getConceptId().equals(conceptId.toString()))
            .collect(Collectors.toList());
    return returnList.isEmpty() ? null : returnList.get(0).getName();
  }

  /**
   * Helper method that generates a list of concept ids per demo
   *
   * @param pageRequest
   */
  private void convertGenderRaceEthnicityFilters(PageRequest pageRequest) {
    List<Filter> filters =
        pageRequest.getFilters().stream()
            .map(
                filter -> {
                  if (GENDER_RACE_ETHNICITY_TYPES.contains(filter.getProperty().name())) {
                    Map<Long, String> possibleConceptIds = new HashMap<>();
                    List<String> values =
                        possibleConceptIds.entrySet().stream()
                            .filter(entry -> filter.getValues().contains(entry.getValue()))
                            .map(entry -> entry.getKey().toString())
                            .collect(Collectors.toList());
                    return new Filter()
                        .property(filter.getProperty())
                        .operator(filter.getOperator())
                        .values(values);
                  }
                  return filter;
                })
            .collect(Collectors.toList());
    pageRequest.filters(filters);
  }

  /**
   * Helper method that converts sortOrder if gender, race or ethnicity.
   *
   * @param pageRequest
   */
  private void convertGenderRaceEthnicitySortOrder(PageRequest pageRequest) {
    String sortColumn = pageRequest.getSortColumn();
    String sortName = pageRequest.getSortOrder().name();
    Sort sort =
        sortName.equalsIgnoreCase(Direction.ASC.toString())
            ? new Sort(Direction.ASC, "name")
            : new Sort(Direction.DESC, "name");
    if (GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
      List<CBCriteria> criteriaList =
          cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
              DomainType.PERSON.toString(), sortColumn, 0L, sort);
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      List<String> demoList =
          criteriaList.stream()
              .map(
                  c ->
                      new ConceptIdName()
                          .conceptId(new Long(c.getConceptId()))
                          .conceptName(c.getName()))
              .sorted(Comparator.comparing(ConceptIdName::getConceptName))
              .map(c -> c.getConceptId().toString())
              .collect(Collectors.toList());
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
    FilterColumns col = request.getDomain() == null ? FilterColumns.PARTICIPANTID : START_DATE;
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

  /**
   * Helper method to convert a collection of {@link FieldValue} to {@link ParticipantData}.
   *
   * @param rm
   * @param row
   * @param domain
   */
  private ParticipantData convertRowToParticipantData(
      Map<String, Integer> rm, List<FieldValue> row, DomainType domain) {
    if (!domain.equals(DomainType.SURVEY)) {
      return new ParticipantData()
          .itemDate(bigQueryService.getDateTime(row, rm.get(START_DATE.toString())))
          .domain(bigQueryService.getString(row, rm.get(DOMAIN.toString())))
          .standardName(bigQueryService.getString(row, rm.get(STANDARD_NAME.toString())))
          .ageAtEvent(bigQueryService.getLong(row, rm.get(AGE_AT_EVENT.toString())).intValue())
          .standardConceptId(bigQueryService.getLong(row, rm.get(STANDARD_CONCEPT_ID.toString())))
          .sourceConceptId(bigQueryService.getLong(row, rm.get(SOURCE_CONCEPT_ID.toString())))
          .standardVocabulary(bigQueryService.getString(row, rm.get(STANDARD_VOCAB.toString())))
          .sourceVocabulary(bigQueryService.getString(row, rm.get(SOURCE_VOCAB.toString())))
          .sourceName(bigQueryService.getString(row, rm.get(SOURCE_NAME.toString())))
          .sourceCode(bigQueryService.getString(row, rm.get(SOURCE_CODE.toString())))
          .standardCode(bigQueryService.getString(row, rm.get(STANDARD_CODE.toString())))
          .value(bigQueryService.getString(row, rm.get(VAL_AS_NUMBER.toString())))
          .visitType(bigQueryService.getString(row, rm.get(VISIT_TYPE.toString())))
          .numMentions(bigQueryService.getString(row, rm.get(NUM_OF_MENTIONS.toString())))
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
          .itemDate(bigQueryService.getDateTime(row, rm.get(START_DATE.toString())))
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
