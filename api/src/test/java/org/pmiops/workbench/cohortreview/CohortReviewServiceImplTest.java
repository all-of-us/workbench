package org.pmiops.workbench.cohortreview;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewServiceImplTest {

    @Mock
    private CohortReviewDao cohortReviewDao;

    @Mock
    private CohortDao cohortDao;

    @Mock
    private ParticipantCohortStatusDao participantCohortStatusDao;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    @InjectMocks
    private CohortReviewServiceImpl cohortReviewService;

    @Test
    public void findCohort_NotFound() throws Exception {
        long cohortId = 1;

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            cohortReviewService.findCohort(cohortId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, e.getMessage());
        }

        verify(cohortDao).findOne(cohortId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohort() throws Exception {
        long cohortId = 1;

        Cohort cohort = new Cohort();
        when(cohortDao.findOne(cohortId)).thenReturn(cohort);

        assertEquals(cohort, cohortReviewService.findCohort(cohortId));

        verify(cohortDao).findOne(cohortId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void validateMatchingWorkspace_NotFound() throws Exception {
        String workspaceNamespace = "test-workspace";
        String workspaceName = "test";
        long workspaceId = 1;
        long badWorkspaceId = 99;

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(badWorkspaceId);
        WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;

        when(workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER)).thenReturn(owner);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);

        try {
            cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceName, workspaceId, WorkspaceAccessLevel.READER);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No workspace matching workspaceNamespace: "
                    + workspaceNamespace + ", workspaceId: " + workspaceName, e.getMessage());
        }
        verify(workspaceService).enforceWorkspaceAccessLevel(workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER);
        verify(workspaceService).getRequired(workspaceNamespace, workspaceName);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void validateMatchingWorkspace() throws Exception {
        String workspaceNamespace = "test-workspace";
        String workspaceName = "test";
        long workspaceId = 1;

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;

        when(workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER)).thenReturn(owner);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);

        cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceName, workspaceId, WorkspaceAccessLevel.READER);

        verify(workspaceService).enforceWorkspaceAccessLevel(workspaceNamespace, workspaceName, WorkspaceAccessLevel.READER);
        verify(workspaceService).getRequired(workspaceNamespace, workspaceName);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortReview_CohortIdAndCdrVersionId_NotFound() throws Exception {
        long cohortReviewId = 1;
        long cdrVersionId = 1;

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId)).thenReturn(null);

        try {
            cohortReviewService.findCohortReview(cohortReviewId, cdrVersionId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: Cohort Review does not exist for cohortId: "
                    + cohortReviewId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao).findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortReview_CohortIdAndCdrVersionId() throws Exception {
        long cohortReviewId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId)).thenReturn(cohortReview);

        CohortReview actualCohortReview = cohortReviewService.findCohortReview(cohortReviewId, cdrVersionId);

        assertEquals(cohortReview, actualCohortReview);

        verify(cohortReviewDao).findCohortReviewByCohortIdAndCdrVersionId(cohortReviewId, cdrVersionId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortReview_CohortReviewId_NotFound() throws Exception {
        long cohortReviewId = 1;

        when(cohortReviewDao.findOne(cohortReviewId)).thenReturn(null);

        try {
            cohortReviewService.findCohortReview(cohortReviewId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: Cohort Review does not exist for cohortReviewId: "
                    + cohortReviewId, e.getMessage());
        }

        verify(cohortReviewDao).findOne(cohortReviewId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findCohortReview_CohortReviewId() throws Exception {
        long cohortReviewId = 1;

        CohortReview cohortReview = new CohortReview();
        when(cohortReviewDao.findOne(cohortReviewId)).thenReturn(cohortReview);

        CohortReview actualCohortReview = cohortReviewService.findCohortReview(cohortReviewId);

        assertEquals(cohortReview, actualCohortReview);

        verify(cohortReviewDao).findOne(cohortReviewId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveCohortReview() throws Exception {
        CohortReview cohortReview = new CohortReview();

        when(cohortReviewDao.save(cohortReview)).thenReturn(cohortReview);

        assertEquals(cohortReview, cohortReviewService.saveCohortReview(cohortReview));

        verify(cohortReviewDao).save(cohortReview);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void saveParticipantCohortStatus() throws Exception {
        ParticipantCohortStatus pcs = new ParticipantCohortStatus();

        when(participantCohortStatusDao.save(pcs)).thenReturn(pcs);

        ParticipantCohortStatus actualPcs = cohortReviewService.saveParticipantCohortStatus(pcs);

        assertEquals(pcs, actualPcs);

        verify(participantCohortStatusDao).save(pcs);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatus_NotFound() throws Exception {
        long cohortReviewId = 1;
        long participantId = 1;

        when(participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId)).thenReturn(null);

        try {
            cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: Participant Cohort Status does not exist for cohortReviewId: "
                    + cohortReviewId + ", participantId: " + participantId, e.getMessage());
        }

        verify(participantCohortStatusDao).findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatus() throws Exception {
        long cohortReviewId = 1;
        long participantId = 1;

        ParticipantCohortStatus pcs = new ParticipantCohortStatus();
        when(participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId)).thenReturn(pcs);

        ParticipantCohortStatus actualpcs = cohortReviewService.findParticipantCohortStatus(cohortReviewId, participantId);

        assertEquals(pcs, actualpcs);

        verify(participantCohortStatusDao).findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                cohortReviewId,
                participantId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findParticipantCohortStatuses() throws Exception {
        long cohortReviewId = 1;
        PageRequest pageRequest = new PageRequest(0, 1);

        ParticipantCohortStatus pcs = new ParticipantCohortStatus();
        when(participantCohortStatusDao.findByParticipantKey_CohortReviewId(
                cohortReviewId,
                pageRequest)).thenReturn(null);

        cohortReviewService.findParticipantCohortStatuses(cohortReviewId, pageRequest);

        verify(participantCohortStatusDao).findByParticipantKey_CohortReviewId(
                cohortReviewId,
                pageRequest);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void findGenderRaceEthnicityFromConcept() throws Exception {
        Concept ethnicityConcept = new Concept()
                .conceptId(1L)
                .conceptName("ethnicity");
        Concept genderConcept = new Concept()
                .conceptId(2L)
                .conceptName("gender");
        Concept raceConcept = new Concept()
                .conceptId(3L)
                .conceptName("race");

        List<Concept> conceptsList = new ArrayList<>();
        conceptsList.add(ethnicityConcept);
        conceptsList.add(genderConcept);
        conceptsList.add(raceConcept);

        Map<String, Map<Long, String>> concepts = new HashMap<>();
        final HashMap<Long, String> race = new HashMap<>();
        race.put(3L, "race");
        final HashMap<Long, String> gender = new HashMap<>();
        gender.put(2L, "gender");
        final HashMap<Long, String> ethnicity = new HashMap<>();
        ethnicity.put(1L, "ethnicity");
        concepts.put(GenderRaceEthnicityType.RACE.name(), race);
        concepts.put(GenderRaceEthnicityType.GENDER.name(), gender);
        concepts.put(GenderRaceEthnicityType.ETHNICITY.name(), ethnicity);

        when(genderRaceEthnicityConceptProvider.get()).thenReturn(new GenderRaceEthnicityConcept(concepts));

        Map<String, Map<Long, String>> conceptList = cohortReviewService.findGenderRaceEthnicityFromConcept();

        assertEquals("ethnicity", conceptList.get(GenderRaceEthnicityType.ETHNICITY.name()).get(ethnicityConcept.getConceptId()));
        assertEquals("gender", conceptList.get(GenderRaceEthnicityType.GENDER.name()).get(genderConcept.getConceptId()));
        assertEquals("race", conceptList.get(GenderRaceEthnicityType.RACE.name()).get(raceConcept.getConceptId()));
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(
                cohortDao,
                cohortReviewDao,
                participantCohortStatusDao,
                workspaceService);
    }

}
