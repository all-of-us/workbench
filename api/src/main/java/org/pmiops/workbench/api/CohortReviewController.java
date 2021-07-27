package org.pmiops.workbench.api;

import static org.pmiops.workbench.model.FilterColumns.START_DATETIME;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Provider;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CohortReviewWithCountResponse;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantDataCountResponse;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
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
  private final UserRecentResourceService userRecentResourceService;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final Clock clock;

  @Autowired
  CohortReviewController(
      CohortReviewService cohortReviewService,
      CohortBuilderService cohortBuilderService,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      Clock clock) {
    this.cohortReviewService = cohortReviewService;
    this.cohortBuilderService = cohortBuilderService;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
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
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    CohortReview cohortReview;
    DbCohort cohort = cohortReviewService.findCohort(dbWorkspace.getWorkspaceId(), cohortId);
    try {
      cohortReview =
          cohortReviewService.findCohortReview(
              cohort.getCohortId(), dbWorkspace.getCdrVersion().getCdrVersionId());
    } catch (NotFoundException nfe) {
      cohortReview = cohortReviewService.initializeCohortReview(cdrVersionId, cohort);
      cohortReview = cohortReviewService.saveCohortReview(cohortReview, userProvider.get());
    }
    if (cohortReview.getReviewSize() > 0) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
              cohortId, cdrVersionId));
    }

    List<DbParticipantCohortStatus> participantCohortStatuses =
        cohortReviewService.createDbParticipantCohortStatusesList(
            cohort, request.getSize(), cohortReview.getCohortReviewId());

    cohortReview
        .reviewSize((long) participantCohortStatuses.size())
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

    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);

    return ResponseEntity.ok(
        cohortReviewService.saveParticipantCohortAnnotation(
            cohortReview.getCohortReviewId(), request));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohortReview(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    cohortReviewService.deleteCohortReview(cohortReview.getCohortReviewId());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId) {

    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    // will throw a NotFoundException if participant cohort annotation does not exist
    cohortReviewService.deleteParticipantCohortAnnotation(
        annotationId, cohortReview.getCohortReviewId(), participantId);

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

    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CohortReview cohortReview = cohortReviewService.findCohortReview(cohortReviewId);
    DbCohort dbCohort =
        cohortReviewService.findCohort(dbWorkspace.getWorkspaceId(), cohortReview.getCohortId());
    return ResponseEntity.ok(
        new CohortChartDataListResponse()
            .count(cohortReview.getMatchedParticipantCount())
            .items(
                cohortReviewService.findCohortChartData(
                    dbCohort, Objects.requireNonNull(Domain.fromValue(domain)), chartLimit)));
  }

  @Override
  public ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
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
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    ParticipantCohortStatus pcs =
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId);

    return ResponseEntity.ok(
        new ParticipantChartDataListResponse()
            .items(
                cohortReviewService.findParticipantChartData(
                    pcs.getParticipantId(),
                    Objects.requireNonNull(Domain.fromValue(domain)),
                    chartLimit)));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(
      String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    List<ParticipantCohortAnnotation> annotations =
        cohortReviewService.findParticipantCohortAnnotations(
            cohortReview.getCohortReviewId(), participantId);

    return ResponseEntity.ok(new ParticipantCohortAnnotationListResponse().items(annotations));
  }

  @Override
  public ResponseEntity<ParticipantCohortStatus> getParticipantCohortStatus(
      String workspaceNamespace, String workspaceId, Long cohortReviewId, Long participantId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    return ResponseEntity.ok(
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId));
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
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    CohortReview cohortReview;
    List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    DbCohort cohort = cohortReviewService.findCohort(dbWorkspace.getWorkspaceId(), cohortId);
    PageRequest pageRequest = createPageRequest(request);
    convertGenderRaceEthnicitySortOrder(pageRequest);

    try {
      cohortReview =
          cohortReviewService.findCohortReview(
              cohort.getCohortId(), dbWorkspace.getCdrVersion().getCdrVersionId());
      participantCohortStatuses =
          cohortReviewService.findAll(cohortReview.getCohortReviewId(), pageRequest);
    } catch (NotFoundException nfe) {
      cohortReview = cohortReviewService.initializeCohortReview(cdrVersionId, cohort);
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
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    ParticipantCohortStatus pcs =
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId);
    return ResponseEntity.ok(
        new ParticipantDataCountResponse()
            .count(
                cohortReviewService.findParticipantCount(
                    pcs.getParticipantId(), request.getDomain(), createPageRequest(request))));
  }

  @Override
  public ResponseEntity<ParticipantDataListResponse> getParticipantData(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      PageFilterRequest request) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    ParticipantCohortStatus pcs =
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId);

    return ResponseEntity.ok(
        new ParticipantDataListResponse()
            .items(
                cohortReviewService.findParticipantData(
                    pcs.getParticipantId(), request.getDomain(), createPageRequest(request))));
  }

  @Override
  public ResponseEntity<VocabularyListResponse> getVocabularies(
      String workspaceNamespace, String workspaceId, Long cohortReviewId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new VocabularyListResponse().items(cohortReviewService.findVocabularies()));
  }

  @Override
  public ResponseEntity<CohortReview> updateCohortReview(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      CohortReview cohortReview) {
    // This also enforces registered auth domain.
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    CohortReview cr =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    return ResponseEntity.ok(
        cohortReviewService.updateCohortReview(
            cohortReview, cr.getCohortReviewId(), new Timestamp(clock.instant().toEpochMilli())));
  }

  @Override
  public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      Long annotationId,
      ModifyParticipantCohortAnnotationRequest request) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    ParticipantCohortStatus pcs =
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId);
    return ResponseEntity.ok(
        cohortReviewService.updateParticipantCohortAnnotation(
            annotationId, cohortReview.getCohortReviewId(), pcs.getParticipantId(), request));
  }

  @Override
  public ResponseEntity<ParticipantCohortStatus> updateParticipantCohortStatus(
      String workspaceNamespace,
      String workspaceId,
      Long cohortReviewId,
      Long participantId,
      ModifyCohortStatusRequest cohortStatusRequest) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    CohortReview cohortReview =
        cohortReviewService.findCohortReviewForWorkspace(
            dbWorkspace.getWorkspaceId(), cohortReviewId);
    ParticipantCohortStatus pcs =
        cohortReviewService.findParticipantCohortStatus(
            cohortReview.getCohortReviewId(), participantId);

    return ResponseEntity.ok(
        cohortReviewService.updateParticipantCohortStatus(
            cohortReview.getCohortReviewId(),
            pcs.getParticipantId(),
            cohortStatusRequest.getStatus(),
            new Timestamp(clock.instant().toEpochMilli())));
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
    int pageParam = Optional.ofNullable(request.getPage()).orElse(PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest()
        .page(pageParam)
        .pageSize(pageSizeParam)
        .sortOrder(sortOrderParam)
        .sortColumn(sortColumn)
        .filters(
            request.getFilters() == null ? new ArrayList<>() : request.getFilters().getItems());
  }
}
