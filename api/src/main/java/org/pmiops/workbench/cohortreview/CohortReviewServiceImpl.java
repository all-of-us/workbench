package org.pmiops.workbench.cohortreview;

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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapper;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CohortReviewServiceImpl implements CohortReviewService, GaugeDataCollector {

  private BigQueryService bigQueryService;
  private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private CohortBuilderService cohortBuilderService;
  private CohortDao cohortDao;
  private CohortReviewDao cohortReviewDao;
  private CohortReviewMapper cohortReviewMapper;
  private CohortQueryBuilder cohortQueryBuilder;
  private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  private ParticipantCohortAnnotationMapper participantCohortAnnotationMapper;
  private ParticipantCohortStatusDao participantCohortStatusDao;
  private ParticipantCohortStatusMapper participantCohortStatusMapper;
  private ReviewQueryBuilder reviewQueryBuilder;
  private Clock clock;

  @Autowired
  public CohortReviewServiceImpl(
      BigQueryService bigQueryService,
      CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao,
      CohortBuilderService cohortBuilderService,
      CohortDao cohortDao,
      CohortReviewDao cohortReviewDao,
      CohortReviewMapper cohortReviewMapper,
      CohortQueryBuilder cohortQueryBuilder,
      ParticipantCohortAnnotationDao participantCohortAnnotationDao,
      ParticipantCohortAnnotationMapper participantCohortAnnotationMapper,
      ParticipantCohortStatusDao participantCohortStatusDao,
      ParticipantCohortStatusMapper participantCohortStatusMapper,
      ReviewQueryBuilder reviewQueryBuilder,
      Clock clock) {
    this.bigQueryService = bigQueryService;
    this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
    this.cohortBuilderService = cohortBuilderService;
    this.cohortDao = cohortDao;
    this.cohortReviewDao = cohortReviewDao;
    this.cohortReviewMapper = cohortReviewMapper;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.participantCohortAnnotationDao = participantCohortAnnotationDao;
    this.participantCohortAnnotationMapper = participantCohortAnnotationMapper;
    this.participantCohortStatusDao = participantCohortStatusDao;
    this.participantCohortStatusMapper = participantCohortStatusMapper;
    this.reviewQueryBuilder = reviewQueryBuilder;
    this.clock = clock;
  }

  public CohortReviewServiceImpl() {}

  @Override
  public DbCohort findCohort(long workspaceId, long cohortId) {
    DbCohort cohort = cohortDao.findCohortByWorkspaceIdAndCohortId(workspaceId, cohortId);
    if (cohort == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
    }
    return cohort;
  }

  @Override
  public CohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
    DbCohortReview cohortReview =
        cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
              cohortId, cdrVersionId));
    }
    return cohortReviewMapper.dbModelToClient(cohortReview);
  }

  @Override
  public CohortReview findCohortReview(Long cohortReviewId) {
    return cohortReviewMapper.dbModelToClient(findDbCohortReview(cohortReviewId));
  }

  @Override
  public CohortReview findCohortReviewForWorkspace(Long workspaceId, Long cohortReviewId) {
    CohortReview cohortReview = findCohortReview(cohortReviewId);
    DbCohort dbCohort =
        cohortDao.findCohortByWorkspaceIdAndCohortId(workspaceId, cohortReview.getCohortId());
    if (dbCohort == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No CohortReview exists for cohortReviewId: %s and cohortId: %s",
              cohortReviewId, cohortReview.getCohortId()));
    }
    return cohortReview;
  }

  @Override
  public void deleteCohortReview(Long cohortReviewId) {
    cohortReviewDao.delete(cohortReviewId);
  }

  @Override
  public List<CohortReview> getRequiredWithCohortReviews(String ns, String firecloudName) {
    return cohortReviewDao
        .findByFirecloudNameAndActiveStatus(
            ns,
            firecloudName,
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE))
        .stream()
        .map(cohortReviewMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CohortReview saveCohortReview(CohortReview cohortReview, DbUser creator) {
    return cohortReviewMapper.dbModelToClient(
        cohortReviewDao.save(cohortReviewMapper.clientToDbModel(cohortReview, creator)));
  }

  @Override
  @Transactional
  public void saveFullCohortReview(
      CohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses) {
    cohortReviewDao.save(cohortReviewMapper.clientToDbModel(cohortReview));
    participantCohortStatusDao.saveParticipantCohortStatusesCustom(participantCohortStatuses);
  }

  public CohortReview updateCohortReview(
      CohortReview cohortReview, Long cohortReviewId, Timestamp lastModified) {
    DbCohortReview dbCohortReview = findDbCohortReview(cohortReviewId);
    if (Strings.isNullOrEmpty(cohortReview.getEtag())) {
      throw new ConflictException("missing required update field 'etag'");
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
    dbCohortReview.setLastModifiedTime(lastModified);
    try {
      return cohortReviewMapper.dbModelToClient(cohortReviewDao.save(dbCohortReview));
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent cohort review modification");
    }
  }

  @Override
  public ParticipantCohortStatus updateParticipantCohortStatus(
      Long cohortReviewId, Long participantId, CohortStatus status, Timestamp lastModified) {
    DbCohortReview dbCohortReview = findDbCohortReview(cohortReviewId);
    dbCohortReview.lastModifiedTime(lastModified);
    dbCohortReview.incrementReviewedCount();
    cohortReviewDao.save(dbCohortReview);

    DbParticipantCohortStatus dbParticipantCohortStatus =
        participantCohortStatusDao
            .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId, participantId);
    if (dbParticipantCohortStatus == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
              cohortReviewId, participantId));
    }
    dbParticipantCohortStatus.setStatusEnum(status);
    return participantCohortStatusMapper.dbModelToClient(
        participantCohortStatusDao.save(dbParticipantCohortStatus),
        cohortBuilderService.findAllDemographicsMap());
  }

  @Override
  public ParticipantCohortStatus findParticipantCohortStatus(
      Long cohortReviewId, Long participantId) {
    DbParticipantCohortStatus dbParticipantCohortStatus =
        participantCohortStatusDao
            .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId, participantId);
    if (dbParticipantCohortStatus == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
              cohortReviewId, participantId));
    }
    return participantCohortStatusMapper.dbModelToClient(
        dbParticipantCohortStatus, cohortBuilderService.findAllDemographicsMap());
  }

  public List<ParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findAll(cohortReviewId, pageRequest).stream()
        .map(
            pcs ->
                participantCohortStatusMapper.dbModelToClient(
                    pcs, cohortBuilderService.findAllDemographicsMap()))
        .collect(Collectors.toList());
  }

  @Override
  public Long findCount(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findCount(cohortReviewId, pageRequest);
  }

  @Override
  public ParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation) {
    DbParticipantCohortAnnotation dbParticipantCohortAnnotation =
        participantCohortAnnotationMapper.clientToDbModel(participantCohortAnnotation);

    DbCohortAnnotationDefinition dbCohortAnnotationDefinition =
        findDbCohortAnnotationDefinition(
            dbParticipantCohortAnnotation.getCohortAnnotationDefinitionId());

    validateParticipantCohortAnnotationAndMutateForSave(
        dbParticipantCohortAnnotation, dbCohortAnnotationDefinition);

    validateParticipantCohortAnnotationNotExists(
        cohortReviewId,
        dbParticipantCohortAnnotation.getCohortAnnotationDefinitionId(),
        dbParticipantCohortAnnotation.getParticipantId());

    return participantCohortAnnotationMapper.dbModelToClient(
        participantCohortAnnotationDao.save(dbParticipantCohortAnnotation));
  }

  @Override
  public ParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyRequest) {
    DbParticipantCohortAnnotation participantCohortAnnotation =
        participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
            annotationId, cohortReviewId, participantId);
    if (participantCohortAnnotation == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Participant Cohort Annotation does not exist for annotationId: %s, cohortReviewId: %s, participantId: %s",
              annotationId, cohortReviewId, participantId));
    }
    participantCohortAnnotation
        .annotationValueString(modifyRequest.getAnnotationValueString())
        .annotationValueEnum(modifyRequest.getAnnotationValueEnum())
        .annotationValueDateString(modifyRequest.getAnnotationValueDate())
        .annotationValueBoolean(modifyRequest.getAnnotationValueBoolean())
        .annotationValueInteger(modifyRequest.getAnnotationValueInteger());
    DbCohortAnnotationDefinition cohortAnnotationDefinition =
        findDbCohortAnnotationDefinition(
            participantCohortAnnotation.getCohortAnnotationDefinitionId());

    validateParticipantCohortAnnotationAndMutateForSave(
        participantCohortAnnotation, cohortAnnotationDefinition);

    return participantCohortAnnotationMapper.dbModelToClient(
        participantCohortAnnotationDao.save(participantCohortAnnotation));
  }

  @Override
  public void deleteParticipantCohortAnnotation(
      Long annotationId, Long cohortReviewId, Long participantId) {
    DbParticipantCohortAnnotation participantCohortAnnotation =
        participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
            annotationId, cohortReviewId, participantId);
    if (participantCohortAnnotation == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No participant cohort annotation found for annotationId: %s,"
                  + " cohortReviewId: %s, participantId: %s",
              annotationId, cohortReviewId, participantId));
    }
    participantCohortAnnotationDao.delete(participantCohortAnnotation);
  }

  @Override
  public List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId) {
    return participantCohortAnnotationDao
        .findByCohortReviewIdAndParticipantId(cohortReviewId, participantId).stream()
        .map(participantCohortAnnotationMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CohortReview initializeCohortReview(Long cdrVersionId, DbCohort dbCohort) {
    SearchRequest request = new Gson().fromJson(getCohortDefinition(dbCohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request))));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    long cohortCount = bigQueryService.getLong(row, rm.get("count"));

    return createNewCohortReview(dbCohort, cdrVersionId, cohortCount);
  }

  @Override
  public List<DbParticipantCohortStatus> createDbParticipantCohortStatusesList(
      DbCohort dbCohort, Integer requestSize, Long cohortReviewId) {
    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(dbCohort), SearchRequest.class);
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildRandomParticipantQuery(
                    new ParticipantCriteria(searchRequest), requestSize, 0L)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<DbParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      participantCohortStatuses.add(
          new DbParticipantCohortStatus()
              .participantKey(
                  new DbParticipantCohortStatusKey(
                      cohortReviewId, bigQueryService.getLong(row, rm.get("person_id"))))
              .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
              .birthDate(getBirthDate(rm, row))
              .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
              .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
              .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id")))
              .sexAtBirthConceptId(bigQueryService.getLong(row, rm.get("sex_at_birth_concept_id")))
              .deceased(bigQueryService.getBoolean(row, rm.get("deceased"))));
    }
    return participantCohortStatuses;
  }

  @Override
  public List<CohortChartData> findCohortChartData(DbCohort dbCohort, Domain domain, int limit) {
    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(dbCohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest), domain, limit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<CohortChartData> cohortChartData = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      cohortChartData.add(
          new CohortChartData()
              .name(bigQueryService.getString(row, rm.get("name")))
              .conceptId(bigQueryService.getLong(row, rm.get("conceptId")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return cohortChartData;
  }

  @Override
  public List<ParticipantChartData> findParticipantChartData(
      Long participantId, Domain domain, int limit) {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildChartDataQuery(participantId, domain, limit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<ParticipantChartData> participantChartData = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      participantChartData.add(
          new ParticipantChartData()
              .standardName(bigQueryService.getString(row, rm.get("standardName")))
              .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
              .startDate(bigQueryService.getDate(row, rm.get("startDate")))
              .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
              .rank(bigQueryService.getLong(row, rm.get("rank")).intValue()));
    }
    return participantChartData;
  }

  @Override
  public Long findParticipantCount(Long participantId, Domain domain, PageRequest pageRequest) {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildCountQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    return bigQueryService.getLong(result.iterateAll().iterator().next(), rm.get("count"));
  }

  @Override
  public List<ParticipantData> findParticipantData(
      Long participantId, Domain domain, PageRequest pageRequest) {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                reviewQueryBuilder.buildQuery(participantId, domain, pageRequest)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<ParticipantData> participantData = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      participantData.add(convertRowToParticipantData(rm, row, domain));
    }
    return participantData;
  }

  @Override
  public List<Vocabulary> findVocabularies() {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(reviewQueryBuilder.buildVocabularyDataQuery()));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<Vocabulary> vocabularies = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      vocabularies.add(
          new Vocabulary()
              .domain(bigQueryService.getString(row, rm.get("domain")))
              .type(bigQueryService.getString(row, rm.get("type")))
              .vocabulary(bigQueryService.getString(row, rm.get("vocabulary"))));
    }
    return vocabularies;
  }

  private DbCohortAnnotationDefinition findDbCohortAnnotationDefinition(
      Long cohortAnnotationDefinitionId) {
    DbCohortAnnotationDefinition cohortAnnotationDefinition =
        cohortAnnotationDefinitionDao.findOne(cohortAnnotationDefinitionId);

    if (cohortAnnotationDefinition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No cohort annotation definition found for id: %s",
              cohortAnnotationDefinitionId));
    }
    return cohortAnnotationDefinition;
  }

  private DbCohortReview findDbCohortReview(Long cohortReviewId) {
    DbCohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortReviewId(cohortReviewId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for cohortReviewId: %s", cohortReviewId));
    }
    return cohortReview;
  }

  private void validateParticipantCohortAnnotationNotExists(
      Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId) {
    if (participantCohortAnnotationDao
            .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                cohortReviewId, cohortAnnotationDefinitionId, participantId)
        != null) {
      throw new ConflictException(
          String.format(
              "Cohort annotation definition exists for id: %s", cohortAnnotationDefinitionId));
    }
  }

  /** Helper method to validate that requested annotations are proper. */
  private void validateParticipantCohortAnnotationAndMutateForSave(
      DbParticipantCohortAnnotation participantCohortAnnotation,
      DbCohortAnnotationDefinition cohortAnnotationDefinition) {

    if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.BOOLEAN)) {
      if (participantCohortAnnotation.getAnnotationValueBoolean() == null) {
        throw createConflictException(
            AnnotationType.BOOLEAN.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.STRING)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueString())) {
        throw createConflictException(
            AnnotationType.STRING.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.DATE)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueDateString())) {
        throw createConflictException(
            AnnotationType.DATE.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
        Date date =
            new Date(
                sdf.parse(participantCohortAnnotation.getAnnotationValueDateString()).getTime());
        participantCohortAnnotation.setAnnotationValueDate(date);
      } catch (ParseException e) {
        throw new BadRequestException(
            String.format(
                "Bad Request: Please provide a valid %s value (%s) for annotation defintion id: %s",
                AnnotationType.DATE.name(),
                sdf.toPattern(),
                participantCohortAnnotation.getCohortAnnotationDefinitionId()));
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.INTEGER)) {
      if (participantCohortAnnotation.getAnnotationValueInteger() == null) {
        throw createConflictException(
            AnnotationType.INTEGER.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.ENUM)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueEnum())) {
        throw createConflictException(
            AnnotationType.ENUM.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      List<DbCohortAnnotationEnumValue> enumValues =
          cohortAnnotationDefinition.getEnumValues().stream()
              .filter(
                  enumValue ->
                      participantCohortAnnotation
                          .getAnnotationValueEnum()
                          .equals(enumValue.getName()))
              .collect(Collectors.toList());
      if (enumValues.isEmpty()) {
        throw createConflictException(
            AnnotationType.ENUM.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      participantCohortAnnotation.setCohortAnnotationEnumValue(enumValues.get(0));
    }
  }

  /** Helper method that creates a {@link ConflictException} from the specified parameters. */
  private ConflictException createConflictException(
      String annotationType, Long cohortAnnotationDefinitionId) {
    return new ConflictException(
        String.format(
            "Conflict Exception: Please provide a valid %s value for annotation defintion id: %s",
            annotationType, cohortAnnotationDefinitionId));
  }

  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a {@link
   * NotFoundException} if {@link DbCohort#getCriteria()} return null.
   */
  private String getCohortDefinition(DbCohort dbCohort) {
    String definition = dbCohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s", dbCohort.getCohortId()));
    }
    return definition;
  }

  @NotNull
  private Date getBirthDate(Map<String, Integer> rm, List<FieldValue> row) {
    String birthDateTimeString = bigQueryService.getString(row, rm.get("birth_datetime"));
    if (birthDateTimeString == null) {
      throw new BigQueryException(
          500, "birth_datetime is null at position: " + rm.get("birth_datetime"));
    }
    return new Date(
        Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000))
            .getTime());
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

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    return ImmutableSet.of(
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.COHORT_COUNT, cohortDao.count())
            .build(),
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.COHORT_REVIEW_COUNT, cohortReviewDao.count())
            .build());
  }
}
