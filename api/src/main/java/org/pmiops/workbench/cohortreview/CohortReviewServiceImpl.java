package org.pmiops.workbench.cohortreview;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.apache.commons.lang3.StringUtils;
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
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ReviewStatus;
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
  private Provider<DbUser> userProvider;

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
      Clock clock,
      Provider<DbUser> userProvider) {
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
    this.userProvider = userProvider;
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
        cohortReviewDao
            .findCohortReviewByCohortIdAndCdrVersionIdOrderByCohortReviewId(cohortId, cdrVersionId)
            .stream()
            .findFirst()
            .orElse(null);

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
    cohortReviewDao.deleteById(cohortReviewId);
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
  public List<CohortReview> getCohortReviewsByCohortId(Long cohortId) {
    return cohortReviewDao.findAllByCohortId(cohortId).stream()
        .map(cohortReviewMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CohortReview saveCohortReview(CohortReview cohortReview, DbUser creator) {
    cohortReview.setLastModifiedBy(userProvider.get().getUsername());
    return cohortReviewMapper.dbModelToClient(
        cohortReviewDao.save(cohortReviewMapper.clientToDbModel(cohortReview, creator)));
  }

  @Override
  @Transactional
  public void saveFullCohortReview(
      CohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses) {
    cohortReview.setLastModifiedBy(userProvider.get().getUsername());
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
    dbCohortReview.setLastModifiedBy(userProvider.get().getUsername());
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
    dbCohortReview.setLastModifiedBy(userProvider.get().getUsername());
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

  @Override
  public Set<Long> findParticipantIdsByCohortReview(Long cohortReviewId) {
    return participantCohortStatusDao.findParticipantIdsByCohortReviewId(cohortReviewId);
  }

  public List<ParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest) {
    Table<Long, CriteriaType, String> demoTable = cohortBuilderService.findAllDemographicsMap();
    return participantCohortStatusDao.findAll(cohortReviewId, pageRequest).stream()
        .map(pcs -> participantCohortStatusMapper.dbModelToClient(pcs, demoTable))
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
        .findByCohortReviewIdAndParticipantId(cohortReviewId, participantId)
        .stream()
        .map(participantCohortAnnotationMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CohortReview initializeCohortReview(Long cdrVersionId, DbCohort dbCohort) {
    return createNewCohortReview(dbCohort, cdrVersionId, participationCount(dbCohort));
  }

  @Override
  public List<DbParticipantCohortStatus> createDbParticipantCohortStatusesList(
      DbCohort dbCohort, Integer requestSize, Long cohortReviewId) {
    CohortDefinition cohortDefinition =
        new Gson().fromJson(getCohortDefinition(dbCohort), CohortDefinition.class);
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            cohortQueryBuilder.buildRandomParticipantQuery(
                new ParticipantCriteria(cohortDefinition), requestSize, 0L));

    return participantCohortStatusMapper.tableResultToDbParticipantCohortStatus(
        result, cohortReviewId);
  }

  @Override
  public Long findParticipantCount(Long participantId, Domain domain, PageRequest pageRequest) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            reviewQueryBuilder.buildCountQuery(participantId, domain, pageRequest));
    FieldValueList row = result.iterateAll().iterator().next();
    return row.get("count").getLongValue();
  }

  @Override
  public List<ParticipantData> findParticipantData(
      Long participantId, Domain domain, PageRequest pageRequest) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            reviewQueryBuilder.buildQuery(participantId, domain, pageRequest));

    return cohortReviewMapper.tableResultToVocabulary(result, domain);
  }

  @Override
  public List<Vocabulary> findVocabularies() {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            reviewQueryBuilder.buildVocabularyDataQuery());
    return cohortReviewMapper.tableResultToVocabulary(result);
  }

  @Override
  public Long participationCount(DbCohort dbCohort) {
    CohortDefinition cohortDefinition =
        new Gson().fromJson(getCohortDefinition(dbCohort), CohortDefinition.class);
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            cohortQueryBuilder.buildParticipantCounterQuery(
                new ParticipantCriteria(cohortDefinition)));
    FieldValueList row = result.iterateAll().iterator().next();
    return row.get("count").getLongValue();
  }

  @Override
  public Optional<DbCohortReview> maybeFindDbCohortReview(Long cohortReviewId) {
    return Optional.ofNullable(cohortReviewDao.findCohortReviewByCohortReviewId(cohortReviewId));
  }

  private DbCohortAnnotationDefinition findDbCohortAnnotationDefinition(
      Long cohortAnnotationDefinitionId) {
    return cohortAnnotationDefinitionDao
        .findById(cohortAnnotationDefinitionId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "Not Found: No cohort annotation definition found for id: %s",
                        cohortAnnotationDefinitionId)));
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
            AnnotationType.BOOLEAN, participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.STRING)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueString())) {
        throw createConflictException(
            AnnotationType.STRING, participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.DATE)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueDateString())) {
        throw createConflictException(
            AnnotationType.DATE, participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      try {
        Date date =
            Date.valueOf(
                LocalDate.parse(
                    participantCohortAnnotation.getAnnotationValueDateString(),
                    DateTimeFormatter.ISO_DATE));
        participantCohortAnnotation.setAnnotationValueDate(date);
      } catch (DateTimeParseException e) {
        throw new BadRequestException(
            String.format(
                "Bad Request: Please provide a valid %s value (yyyy-MM-dd) for annotation definition id: %s",
                AnnotationType.DATE,
                participantCohortAnnotation.getCohortAnnotationDefinitionId()));
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.INTEGER)) {
      if (participantCohortAnnotation.getAnnotationValueInteger() == null) {
        throw createConflictException(
            AnnotationType.INTEGER, participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.ENUM)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueEnum())) {
        throw createConflictException(
            AnnotationType.ENUM, participantCohortAnnotation.getCohortAnnotationDefinitionId());
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
            AnnotationType.ENUM, participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      participantCohortAnnotation.setCohortAnnotationEnumValue(enumValues.get(0));
    }
  }

  /** Helper method that creates a {@link ConflictException} from the specified parameters. */
  private ConflictException createConflictException(
      AnnotationType annotationType, Long cohortAnnotationDefinitionId) {
    return new ConflictException(
        String.format(
            "Conflict Exception: Please provide a valid %s value for annotation definition id: %s",
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
