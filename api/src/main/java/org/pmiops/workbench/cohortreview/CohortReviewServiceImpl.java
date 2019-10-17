package org.pmiops.workbench.cohortreview;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CohortReviewServiceImpl implements CohortReviewService {

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
  public Cohort findCohort(long cohortId) {
    Cohort cohort = cohortDao.findOne(cohortId);
    if (cohort == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
    }
    return cohort;
  }

  @Override
  public Workspace validateMatchingWorkspaceAndSetCdrVersion(
      String workspaceNamespace,
      String workspaceName,
      long workspaceId,
      WorkspaceAccessLevel accessRequired) {
    Workspace workspace =
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
  public CohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
    CohortReview cohortReview =
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
  public CohortReview findCohortReview(Long cohortReviewId) {
    CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortReviewId(cohortReviewId);

    if (cohortReview == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: Cohort Review does not exist for cohortReviewId: %s", cohortReviewId));
    }
    return cohortReview;
  }

  @Override
  public CohortReview findCohortReview(String ns, String firecloudName, Long cohortReviewId) {
    CohortReview cohortReview =
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
  public void deleteCohortReview(CohortReview cohortReview) {
    cohortReviewDao.delete(cohortReview);
  }

  @Override
  public List<CohortReview> getRequiredWithCohortReviews(String ns, String firecloudName) {
    return cohortReviewDao.findByFirecloudNameAndActiveStatus(
        ns,
        firecloudName,
        StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  @Override
  public CohortReview saveCohortReview(CohortReview cohortReview) {
    return cohortReviewDao.save(cohortReview);
  }

  @Override
  @Transactional
  public void saveFullCohortReview(
      CohortReview cohortReview, List<ParticipantCohortStatus> participantCohortStatuses) {
    saveCohortReview(cohortReview);
    participantCohortStatusDao.saveParticipantCohortStatusesCustom(participantCohortStatuses);
  }

  @Override
  public ParticipantCohortStatus saveParticipantCohortStatus(
      ParticipantCohortStatus participantCohortStatus) {
    return participantCohortStatusDao.save(participantCohortStatus);
  }

  @Override
  public ParticipantCohortStatus findParticipantCohortStatus(
      Long cohortReviewId, Long participantId) {
    ParticipantCohortStatus participantCohortStatus =
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

  public List<ParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findAll(cohortReviewId, pageRequest);
  }

  @Override
  public Long findCount(Long cohortReviewId, PageRequest pageRequest) {
    return participantCohortStatusDao.findCount(cohortReviewId, pageRequest);
  }

  @Override
  public ParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation) {
    CohortAnnotationDefinition cohortAnnotationDefinition =
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
  public ParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyRequest) {
    ParticipantCohortAnnotation participantCohortAnnotation =
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
    CohortAnnotationDefinition cohortAnnotationDefinition =
        findCohortAnnotationDefinition(
            participantCohortAnnotation.getCohortAnnotationDefinitionId());

    validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition);

    return participantCohortAnnotationDao.save(participantCohortAnnotation);
  }

  @Override
  public CohortAnnotationDefinition findCohortAnnotationDefinition(
      Long cohortAnnotationDefinitionId) {
    CohortAnnotationDefinition cohortAnnotationDefinition =
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
    ParticipantCohortAnnotation participantCohortAnnotation =
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
  public ParticipantCohortAnnotation findParticipantCohortAnnotation(
      Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId) {
    return participantCohortAnnotationDao
        .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
            cohortReviewId, cohortAnnotationDefinitionId, participantId);
  }

  @Override
  public List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(
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
      ParticipantCohortAnnotation participantCohortAnnotation,
      CohortAnnotationDefinition cohortAnnotationDefinition) {

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
      List<CohortAnnotationEnumValue> enumValues =
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
}
