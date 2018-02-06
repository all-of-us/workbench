package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;
import java.util.List;
import java.util.logging.Logger;

@Service
public class CohortReviewServiceImpl implements CohortReviewService {

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private WorkspaceService workspaceService;
    private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    private static final Logger log = Logger.getLogger(CohortReviewServiceImpl.class.getName());

    @Autowired
    CohortReviewServiceImpl(CohortReviewDao cohortReviewDao,
                            CohortDao cohortDao,
                            ParticipantCohortStatusDao participantCohortStatusDao,
                            WorkspaceService workspaceService,
                            Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.participantCohortStatusDao = participantCohortStatusDao;
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
    public CohortReview findCohortReview(Long cohortReviewId) {
        CohortReview cohortReview = cohortReviewDao.findOne(cohortReviewId);

        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortReviewId: %s",
                            cohortReviewId));
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
        cohortReview = saveCohortReview(cohortReview);
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

    public List<ParticipantCohortStatus> findAll(Long cohortReviewId, List<Filter> filterList, PageRequest pageRequest) {
        return participantCohortStatusDao.findAll(cohortReviewId, filterList, pageRequest);
    }
}
