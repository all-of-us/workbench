package org.pmiops.workbench.cohortreview;

import com.google.common.collect.ImmutableSet;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CohortReviewServiceImpl implements CohortReviewService, GaugeDataCollector {

  private CohortReviewDao cohortReviewDao;
  private CohortDao cohortDao;
  private ParticipantCohortStatusDao participantCohortStatusDao;
  private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private WorkspaceService workspaceService;

  @Autowired
  public CohortReviewServiceImpl(
      CohortReviewDao cohortReviewDao,
      CohortDao cohortDao,
      ParticipantCohortStatusDao participantCohortStatusDao,
      ParticipantCohortAnnotationDao participantCohortAnnotationDao,
      CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao,
      WorkspaceService workspaceService) {
    this.cohortReviewDao = cohortReviewDao;
    this.cohortDao = cohortDao;
    this.participantCohortStatusDao = participantCohortStatusDao;
    this.participantCohortAnnotationDao = participantCohortAnnotationDao;
    this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
    this.workspaceService = workspaceService;
  }

  public CohortReviewServiceImpl() {}

  @Override
  public DbCohort findCohort(long cohortId) {
    DbCohort cohort = cohortDao.findOne(cohortId);
    if (cohort == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
    }
    return cohort;
  }

  @Override
  public DbWorkspace validateMatchingWorkspaceAndSetCdrVersion(
      String workspaceNamespace,
      String workspaceName,
      long workspaceId,
      WorkspaceAccessLevel accessRequired) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceName, accessRequired);
    if (workspace == null || workspace.getWorkspaceId() != workspaceId) {
      throw new NotFoundException(
          String.format(
              "Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
              workspaceNamespace, workspaceName));
    }
    return workspace;
  }

  @Override
  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
    return workspaceService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, requiredAccess);
  }

  @Override
  public DbCohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
    DbCohortReview cohortReview =
        cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
              cohortId, cdrVersionId));
    }
    return cohortReview;
  }

  @Override
  public DbCohortReview findCohortReview(Long cohortReviewId) {
    DbCohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortReviewId(cohortReviewId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for cohortReviewId: %s", cohortReviewId));
    }
    return cohortReview;
  }

  @Override
  public DbCohortReview findCohortReview(String ns, String firecloudName, Long cohortReviewId) {
    DbCohortReview cohortReview =
        cohortReviewDao.findByNamespaceAndFirecloudNameAndCohortReviewId(
            ns, firecloudName, cohortReviewId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for namespace: %s, firecloudName: %s and cohortReviewId: %d",
              ns, firecloudName, cohortReviewId));
    }
    return cohortReview;
  }

  @Override
  public void deleteCohortReview(DbCohortReview cohortReview) {
    cohortReviewDao.delete(cohortReview);
  }

  @Override
  public List<DbCohortReview> getRequiredWithCohortReviews(String ns, String firecloudName) {
    return cohortReviewDao.findByFirecloudNameAndActiveStatus(
        ns,
        firecloudName,
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  @Override
  public DbCohortReview saveCohortReview(DbCohortReview cohortReview) {
    return cohortReviewDao.save(cohortReview);
  }

  @Override
  @Transactional
  public void saveFullCohortReview(
      DbCohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses) {
    saveCohortReview(cohortReview);
    participantCohortStatusDao.saveParticipantCohortStatusesCustom(participantCohortStatuses);
  }

  @Override
  public DbParticipantCohortStatus saveParticipantCohortStatus(
      DbParticipantCohortStatus participantCohortStatus) {
    return participantCohortStatusDao.save(participantCohortStatus);
  }

  @Override
  public DbParticipantCohortStatus findParticipantCohortStatus(
      Long cohortReviewId, Long participantId) {
    DbParticipantCohortStatus participantCohortStatus =
        participantCohortStatusDao
            .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId, participantId);
    if (participantCohortStatus == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
              cohortReviewId, participantId));
    }
    return participantCohortStatus;
  }

  public List<DbParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findAll(cohortReviewId, pageRequest);
  }

  @Override
  public Long findCount(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findCount(cohortReviewId, pageRequest);
  }

  @Override
  public DbParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, DbParticipantCohortAnnotation participantCohortAnnotation) {
    DbCohortAnnotationDefinition cohortAnnotationDefinition =
        findCohortAnnotationDefinition(
            participantCohortAnnotation.getCohortAnnotationDefinitionId());

    validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition);

    if (findParticipantCohortAnnotation(
            cohortReviewId,
            participantCohortAnnotation.getCohortAnnotationDefinitionId(),
            participantCohortAnnotation.getParticipantId())
        != null) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Cohort annotation definition exists for id: %s",
              participantCohortAnnotation.getCohortAnnotationDefinitionId()));
    }
    return participantCohortAnnotationDao.save(participantCohortAnnotation);
  }

  @Override
  public DbParticipantCohortAnnotation updateParticipantCohortAnnotation(
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
        findCohortAnnotationDefinition(
            participantCohortAnnotation.getCohortAnnotationDefinitionId());

    validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition);

    return participantCohortAnnotationDao.save(participantCohortAnnotation);
  }

  @Override
  public DbCohortAnnotationDefinition findCohortAnnotationDefinition(
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
  public DbParticipantCohortAnnotation findParticipantCohortAnnotation(
      Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId) {
    return participantCohortAnnotationDao
        .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
            cohortReviewId, cohortAnnotationDefinitionId, participantId);
  }

  @Override
  public List<DbParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId) {
    return participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(
        cohortReviewId, participantId);
  }

  /**
   * Helper method to validate that requested annotations are proper.
   *
   * @param participantCohortAnnotation
   */
  private void validateParticipantCohortAnnotation(
      DbParticipantCohortAnnotation participantCohortAnnotation,
      DbCohortAnnotationDefinition cohortAnnotationDefinition) {

    if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.BOOLEAN)) {
      if (participantCohortAnnotation.getAnnotationValueBoolean() == null) {
        throw createBadRequestException(
            AnnotationType.BOOLEAN.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.STRING)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueString())) {
        throw createBadRequestException(
            AnnotationType.STRING.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.DATE)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueDateString())) {
        throw createBadRequestException(
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
        throw createBadRequestException(
            AnnotationType.INTEGER.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
    } else if (cohortAnnotationDefinition.getAnnotationTypeEnum().equals(AnnotationType.ENUM)) {
      if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueEnum())) {
        throw createBadRequestException(
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
        throw createBadRequestException(
            AnnotationType.ENUM.name(),
            participantCohortAnnotation.getCohortAnnotationDefinitionId());
      }
      participantCohortAnnotation.setCohortAnnotationEnumValue(enumValues.get(0));
    }
  }

  /**
   * Helper method that creates a {@link BadRequestException} from the specified parameters.
   *
   * @param annotationType
   * @param cohortAnnotationDefinitionId
   * @return
   */
  private BadRequestException createBadRequestException(
      String annotationType, Long cohortAnnotationDefinitionId) {
    return new BadRequestException(
        String.format(
            "Bad Request: Please provide a valid %s value for annotation defintion id: %s",
            annotationType, cohortAnnotationDefinitionId));
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
