package org.pmiops.workbench.cohortreview;

import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class CohortReviewServiceImpl implements CohortReviewService {

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
    private WorkspaceService workspaceService;
    private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    private static final Logger log = Logger.getLogger(CohortReviewServiceImpl.class.getName());

    @Autowired
    CohortReviewServiceImpl(CohortReviewDao cohortReviewDao,
                            CohortDao cohortDao,
                            ParticipantCohortStatusDao participantCohortStatusDao,
                            ParticipantCohortAnnotationDao participantCohortAnnotationDao,
                            CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao,
                            WorkspaceService workspaceService,
                            Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.participantCohortStatusDao = participantCohortStatusDao;
        this.participantCohortAnnotationDao = participantCohortAnnotationDao;
        this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
        this.workspaceService = workspaceService;
        this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
    }

    public CohortReviewServiceImpl() {
    }

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
    public Workspace validateMatchingWorkspace(
        String workspaceNamespace, String workspaceName,
        long workspaceId, WorkspaceAccessLevel accessRequired) {
      // This also enforces registered auth domain.
      workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace, workspaceName, accessRequired);


      Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceName);
      if (workspace.getWorkspaceId() != workspaceId) {
          throw new NotFoundException(
                  String.format("Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                          workspaceNamespace, workspaceName));
      }
      return workspace;
    }

    @Override
    public CohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId));
        }
        return cohortReview;
    }

    @Override
    public CohortReview saveCohortReview(CohortReview cohortReview) {
        return cohortReviewDao.save(cohortReview);
    }

    @Override
    @Transactional
    public void saveFullCohortReview(CohortReview cohortReview, List<ParticipantCohortStatus> participantCohortStatuses) {
        saveCohortReview(cohortReview);
        participantCohortStatusDao.saveParticipantCohortStatusesCustom(participantCohortStatuses);
    }

    @Override
    public ParticipantCohortStatus saveParticipantCohortStatus(ParticipantCohortStatus participantCohortStatus) {
        return participantCohortStatusDao.save(participantCohortStatus);
    }

    @Override
    public ParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId) {
        ParticipantCohortStatus participantCohortStatus =
                participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReviewId,
                        participantId);
        if (participantCohortStatus == null) {
            throw new NotFoundException(
                    String.format("Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
                            cohortReviewId, participantId));
        }
        return participantCohortStatus;
    }

    @Override
    public List<ParticipantCohortStatus> findAll(Long cohortReviewId, List<Filter> filterList, PageRequest pageRequest) {
        return participantCohortStatusDao.findAll(cohortReviewId, filterList, pageRequest);
    }

    @Override
    public ParticipantCohortAnnotation saveParticipantCohortAnnotation(Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation) {
        CohortAnnotationDefinition cohortAnnotationDefinition =
                findCohortAnnotationDefinition(participantCohortAnnotation.getCohortAnnotationDefinitionId());

        validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition);

        if(findParticipantCohortAnnotation(cohortReviewId,
                participantCohortAnnotation.getCohortAnnotationDefinitionId(),
                participantCohortAnnotation.getParticipantId()) != null) {
            throw new BadRequestException(
                    String.format("Invalid Request: Cohort annotation definition exists for id: %s",
                            participantCohortAnnotation.getCohortAnnotationDefinitionId()));
        }
        return participantCohortAnnotationDao.save(participantCohortAnnotation);
    }

    @Override
    public ParticipantCohortAnnotation updateParticipantCohortAnnotation(Long annotationId, Long cohortReviewId, Long participantId,
                                                                         ModifyParticipantCohortAnnotationRequest modifyRequest) {
        ParticipantCohortAnnotation participantCohortAnnotation =
                participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(annotationId, cohortReviewId, participantId);
        if (participantCohortAnnotation == null) {
            throw new NotFoundException(
                    String.format("Not Found: Participant Cohort Annotation does not exist for annotationId: %s, cohortReviewId: %s, participantId: %s",
                            annotationId, cohortReviewId, participantId));
        }
        participantCohortAnnotation.annotationValueString(modifyRequest.getValueString())
                .annotationValueEnum(modifyRequest.getValueEnum())
                .annotationValueDateString(modifyRequest.getValueDate())
                .annotationValueBoolean(modifyRequest.getValueBoolean())
                .annotationValueInteger(modifyRequest.getValueInteger());
        CohortAnnotationDefinition cohortAnnotationDefinition =
                findCohortAnnotationDefinition(participantCohortAnnotation.getCohortAnnotationDefinitionId());

        validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition);

        return participantCohortAnnotationDao.save(participantCohortAnnotation);
    }

    @Override
    public CohortAnnotationDefinition findCohortAnnotationDefinition(Long cohortAnnotationDefinitionId) {
        CohortAnnotationDefinition cohortAnnotationDefinition = cohortAnnotationDefinitionDao.findOne(cohortAnnotationDefinitionId);

        if (cohortAnnotationDefinition == null) {
            throw new NotFoundException(
                    String.format("Not Found: No cohort annotation definition found for id: %s", cohortAnnotationDefinitionId));
        }
        return cohortAnnotationDefinition;
    }

    @Override
    public void deleteParticipantCohortAnnotation(Long annotationId, Long cohortReviewId, Long participantId) {
        ParticipantCohortAnnotation participantCohortAnnotation =
                participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                        annotationId,
                        cohortReviewId,
                        participantId);
        if (participantCohortAnnotation == null) {
            throw new NotFoundException(
                    String.format("Not Found: No participant cohort annotation found for annotationId: %s," +
                            " cohortReviewId: %s, participantId: %s", annotationId, cohortReviewId, participantId));
        }
        participantCohortAnnotationDao.delete(participantCohortAnnotation);
    }

    @Override
    public ParticipantCohortAnnotation findParticipantCohortAnnotation(Long cohortReviewId,
                                                                       Long cohortAnnotationDefinitionId,
                                                                       Long participantId) {
        return participantCohortAnnotationDao.findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                cohortReviewId,
                cohortAnnotationDefinitionId,
                participantId);
    }

    @Override
    public List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(Long cohortReviewId, Long participantId) {
        return participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(cohortReviewId, participantId);
    }

    /**
     * Helper method to validate that requested annotations are proper.
     *
     * @param participantCohortAnnotation
     */
    private void validateParticipantCohortAnnotation(ParticipantCohortAnnotation participantCohortAnnotation,
                                                     CohortAnnotationDefinition cohortAnnotationDefinition) {

        if (cohortAnnotationDefinition.getAnnotationType().equals(AnnotationType.BOOLEAN)) {
            if (participantCohortAnnotation.getAnnotationValueBoolean() == null) {
                throw createBadRequestException(AnnotationType.BOOLEAN.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
        } else if (cohortAnnotationDefinition.getAnnotationType().equals(AnnotationType.STRING)) {
            if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueString())) {
                throw createBadRequestException(AnnotationType.STRING.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
        } else if (cohortAnnotationDefinition.getAnnotationType().equals(AnnotationType.DATE)) {
            if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueDateString())) {
                throw createBadRequestException(AnnotationType.DATE.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = new Date(sdf.parse(participantCohortAnnotation.getAnnotationValueDateString()).getTime());
                participantCohortAnnotation.setAnnotationValueDate(date);
            } catch (ParseException e) {
                throw new BadRequestException(String.format("Invalid Request: Please provide a valid %s value (%s) for annotation defintion id: %s",
                        AnnotationType.DATE.name(),
                        sdf.toPattern(),
                        participantCohortAnnotation.getCohortAnnotationDefinitionId()));
            }
        } else if (cohortAnnotationDefinition.getAnnotationType().equals(AnnotationType.INTEGER)) {
            if (participantCohortAnnotation.getAnnotationValueInteger() == null) {
                throw createBadRequestException(AnnotationType.INTEGER.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
        } else if (cohortAnnotationDefinition.getAnnotationType().equals(AnnotationType.ENUM)) {
            if (StringUtils.isBlank(participantCohortAnnotation.getAnnotationValueEnum())) {
                throw createBadRequestException(AnnotationType.ENUM.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
            List<CohortAnnotationEnumValue> enumValues = cohortAnnotationDefinition.getEnumValues().stream()
                    .filter(enumValue -> participantCohortAnnotation.getAnnotationValueEnum().equals(enumValue.getName()))
                    .collect(Collectors.toList());
            if (enumValues.isEmpty()) {
                throw createBadRequestException(AnnotationType.ENUM.name(), participantCohortAnnotation.getCohortAnnotationDefinitionId());
            }
            participantCohortAnnotation.setCohortAnnotationEnumValueId(enumValues.get(0).getCohortAnnotationEnumValueId());
        }
    }

    /**
     * Helper method that creates a {@link BadRequestException} from the specified parameters.
     *
     * @param annotationType
     * @param cohortAnnotationDefinitionId
     * @return
     */
    private BadRequestException createBadRequestException(String annotationType, Long cohortAnnotationDefinitionId) {
        return new BadRequestException(
                String.format("Invalid Request: Please provide a valid %s value for annotation defintion id: %s", annotationType, cohortAnnotationDefinitionId)
        );
    }
}
