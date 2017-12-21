package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CohortReviewServiceImpl implements CohortReviewService {

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private WorkspaceService workspaceService;

    @Autowired
    CohortReviewServiceImpl(CohortReviewDao cohortReviewDao,
                            CohortDao cohortDao,
                            ParticipantCohortStatusDao participantCohortStatusDao,
                            WorkspaceService workspaceService) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.participantCohortStatusDao = participantCohortStatusDao;
        this.workspaceService = workspaceService;
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
    public void validateMatchingWorkspace(String workspaceNamespace, String workspaceName, long workspaceId) {
        Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceName);
        if (workspace.getWorkspaceId() != workspaceId) {
            throw new NotFoundException(
                    String.format("Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName));
        }
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
    public Iterable<ParticipantCohortStatus> saveParticipantCohortStatuses(List<ParticipantCohortStatus> participantCohortStatuses) {
        return participantCohortStatusDao.save(participantCohortStatuses);
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
    public Slice<ParticipantCohortStatus> findParticipantCohortStatuses(Long cohortReviewId, PageRequest pageRequest) {
        return participantCohortStatusDao.findByParticipantKey_CohortReviewId(cohortReviewId, pageRequest);
    }
}
