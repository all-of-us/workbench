package org.pmiops.workbench.cohortreview;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

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
    private JdbcTemplate jdbcTemplate;

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
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);

        try {
            cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceName, workspaceId);
            fail("Should have thrown NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No workspace matching workspaceNamespace: "
                    + workspaceNamespace + ", workspaceId: " + workspaceName, e.getMessage());
        }

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
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);

        cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceName, workspaceId);

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
    public void saveParticipantCohortStatuses() throws Exception {
//        Whitebox.setInternalState(cohortReviewService, "entityManager", entityManager);
//        Whitebox.setInternalState(cohortReviewService, "batchSize", 50);
//        DataSource mockDatasource = mock(DataSource.class);
//        Connection mockConnection = mock(Connection.class);
//        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
//
//        String compiledQuery = "insert into participant_cohort_status(" +
//                "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, " +
//                "status, cohort_review_id, participant_id)" +
//                " values (?, ?, ?, ?, ?, ?, ?)";
//
//        List<ParticipantCohortStatus> pcsList = new ArrayList<>();
//        final ParticipantCohortStatus participantCohortStatus = new ParticipantCohortStatus();
//        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey(1, 1);
//        participantCohortStatus.setParticipantKey(key);
//        participantCohortStatus.setEthnicityConceptId(1L);
//        participantCohortStatus.setGenderConceptId(1L);
//        participantCohortStatus.setRaceConceptId(1L);
//        pcsList.add(participantCohortStatus);
//
//        when(jdbcTemplate.getDataSource()).thenReturn(mockDatasource);
//        when(mockDatasource.getConnection()).thenReturn(mockConnection);
//        doNothing().when(mockConnection).setAutoCommit(false);
//        when(mockConnection.prepareStatement(compiledQuery)).thenReturn(mockPreparedStatement);
//        doNothing().when(mockPreparedStatement).setDate(1, participantCohortStatus.getBirthDate());
//        doNothing().when(mockPreparedStatement).setLong(2, participantCohortStatus.getEthnicityConceptId());
//        doNothing().when(mockPreparedStatement).setLong(3, participantCohortStatus.getGenderConceptId());
//        doNothing().when(mockPreparedStatement).setLong(4, participantCohortStatus.getRaceConceptId());
//        doNothing().when(mockPreparedStatement).setInt(5, 0);
//        doNothing().when(mockPreparedStatement).setLong(6, participantCohortStatus.getParticipantKey().getCohortReviewId());
//        doNothing().when(mockPreparedStatement).setLong(7, participantCohortStatus.getParticipantKey().getParticipantId());
//        doNothing().when(mockPreparedStatement).addBatch();
//        when(mockPreparedStatement.executeBatch()).thenReturn(new int[]{1});
//        doNothing().when(mockPreparedStatement).close();
//        doNothing().when(mockConnection).close();
//
//        cohortReviewService.saveParticipantCohortStatuses(pcsList);
//
//        verify(jdbcTemplate).getDataSource();
//        verify(mockDatasource).getConnection();
//        verify(mockConnection).setAutoCommit(false);
//        verify(mockConnection).prepareStatement(compiledQuery);
//        verify(mockPreparedStatement).setDate(1, participantCohortStatus.getBirthDate());
//        verify(mockPreparedStatement).setLong(2, participantCohortStatus.getEthnicityConceptId());
//        verify(mockPreparedStatement).setLong(3, participantCohortStatus.getGenderConceptId());
//        verify(mockPreparedStatement).setLong(4, participantCohortStatus.getRaceConceptId());
//        verify(mockPreparedStatement).setInt(5, 0);
//        verify(mockPreparedStatement).setLong(6, participantCohortStatus.getParticipantKey().getCohortReviewId());
//        verify(mockPreparedStatement).setLong(7, participantCohortStatus.getParticipantKey().getParticipantId());
//        verify(mockPreparedStatement).addBatch();
//        verify(mockPreparedStatement).executeBatch();
//        verify(mockPreparedStatement).close();
//        verify(mockConnection).close();
//        verifyNoMoreMockInteractions();
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

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortDao, cohortReviewDao, participantCohortStatusDao, workspaceService);
    }

}
