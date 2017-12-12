package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewControllerTest {

    @Mock
    ParticipantCohortStatusDao participantCohortStatusDao;

    @Mock
    CohortReviewDao cohortReviewDao;

    @Mock
    CohortDao cohortDao;

    @Mock
    BigQueryService bigQueryService;

    @Mock
    ParticipantCounter participantCounter;

    @Mock
    WorkspaceService workspaceService;

    @Mock
    CodeDomainLookupService codeDomainLookupService;

    @InjectMocks
    CohortReviewController reviewController;

    @Test
    public void createCohortReview_ReviewAlreadyCreated() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        cohortReview.setReviewSize(1);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review already created for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void createCohortReview_MoreThanTenThousand() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(20000));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review size must be between 0 and 10000", e.getMessage());
        }

        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void createCohortReview_BadCohortIdOrCdrVersionId() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review does not exist for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void createCohortReview_NoCohortDefinitionFound() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortId(cohortId);
        Cohort cohort = new Cohort();
        cohort.setCohortId(cohortId);
        cohort.setWorkspaceId(0);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(0);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort definition matching cohortId: "
                         + cohortId,
                         e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter, workspaceService);
    }

    @Test
    public void createCohortReview() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long cohortReviewId = 1;

        CohortReview cohortReview = new CohortReview();
        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(0);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(0);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        cohortReview.setCohortReviewId(cohortReviewId);
        cohortReview.setCohortId(cohortId);
        cohortReview.setCdrVersionId(cdrVersionId);
        cohortReview.setMatchedParticipantCount(1000);
        cohortReview.setReviewSize(0);
        cohortReview.setCreationTime(new Timestamp(System.currentTimeMillis()));

        CohortReview cohortReviewAfter = new CohortReview();
        cohortReviewAfter.setCohortReviewId(cohortReviewId);
        cohortReviewAfter.setCohortId(cohortId);
        cohortReviewAfter.setCdrVersionId(cdrVersionId);
        cohortReviewAfter.setMatchedParticipantCount(1000);
        cohortReviewAfter.setReviewSize(1);
        cohortReviewAfter.setCreationTime(new Timestamp(System.currentTimeMillis()));
        cohortReviewAfter.setReviewStatus(ReviewStatus.CREATED);

        String definition = "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                            "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                            "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";

        cohort.setCriteria(definition);

        SearchRequest request = new Gson().fromJson(definition, SearchRequest.class);
        QueryResult queryResult = mock(QueryResult.class);
        Iterable testIterable = new Iterable() {
            @Override
            public Iterator iterator() {
                List<FieldValue> list = new ArrayList<>();
                list.add(null);
                return list.iterator();
            }
        };
        Map<String, Integer> rm = new HashMap<>();
        rm.put("person_id", 0);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(namespace, name)).thenReturn(workspace);
        when(participantCounter.buildParticipantIdQuery(request, 200, 0L)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(0L);
        when(cohortReviewDao.save(cohortReviewAfter)).thenReturn(cohortReviewAfter);

        reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(namespace, name);
        verify(participantCounter, times(1)).buildParticipantIdQuery(request, 200, 0L);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verify(cohortReviewDao, times(1)).save(cohortReviewAfter);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void getParticipants() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1L;
        long cdrVersionId = 1L;
        int page = 1;
        int limit = 22;
        String order = "desc";
        String column = "status";

        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, null, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, limit, null, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, order, null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null, null, column);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, limit, order, "participantId");
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, limit, order, column);
    }

    @Test
    public void updateParticipantCohortStatus_BadCohortId() {
        String workspaceNamespace = "aou-test";
        String workspaceName = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long participantId = 1;
        ModifyCohortStatusRequest cohortStatusRequest = new ModifyCohortStatusRequest();

        when(cohortDao.findOne(cohortId)).thenReturn(null);

        try {
            reviewController.updateParticipantCohortStatus(
                    workspaceNamespace,
                    workspaceName,
                    cohortId,
                    cdrVersionId,
                    participantId,
                    cohortStatusRequest);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort exists for cohortId: "
                    + cohortId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void updateParticipantCohortStatus_BadWorkspaceNameOrNamespace() {
        String workspaceNamespace = "aou-test";
        String workspaceName = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long participantId = 1;
        long workspaceId = 1;
        ModifyCohortStatusRequest cohortStatusRequest = new ModifyCohortStatusRequest();

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(2);
        workspace.setWorkspaceNamespace(workspaceNamespace);
        workspace.setFirecloudName("bad" + workspaceName);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);

        try {
            reviewController.updateParticipantCohortStatus(
                    workspaceNamespace,
                    workspaceName,
                    cohortId,
                    cdrVersionId,
                    participantId,
                    cohortStatusRequest);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No workspace matching workspaceNamespace: "
                    + workspaceNamespace + ", workspaceId: " + workspaceName, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(workspaceNamespace, workspaceName);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void updateParticipantCohortStatus_BadCohortReview() {
        String workspaceNamespace = "aou-test";
        String workspaceName = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long participantId = 1;
        long workspaceId = 1;
        ModifyCohortStatusRequest cohortStatusRequest = new ModifyCohortStatusRequest();

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(1);
        workspace.setWorkspaceNamespace(workspaceNamespace);
        workspace.setFirecloudName(workspaceName);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);
        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);

        try {
            reviewController.updateParticipantCohortStatus(
                    workspaceNamespace,
                    workspaceName,
                    cohortId,
                    cdrVersionId,
                    participantId,
                    cohortStatusRequest);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review does not exist for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(workspaceNamespace, workspaceName);
        verify(cohortReviewDao, times(1))
                .findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void updateParticipantCohortStatus_BadParticipantCohortStatus() {
        String workspaceNamespace = "aou-test";
        String workspaceName = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long participantId = 1;
        long workspaceId = 1;
        ModifyCohortStatusRequest cohortStatusRequest = new ModifyCohortStatusRequest();

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(workspaceNamespace);
        workspace.setFirecloudName(workspaceName);

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortReviewId(1);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);
        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(participantCohortStatusDao
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReview.getCohortReviewId(), participantId)).thenReturn(null);

        try {
            reviewController.updateParticipantCohortStatus(
                    workspaceNamespace,
                    workspaceName,
                    cohortId,
                    cdrVersionId,
                    participantId,
                    cohortStatusRequest);
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No participant exists for participantId: "
                    + participantId, e.getMessage());
        }

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(workspaceNamespace, workspaceName);
        verify(cohortReviewDao, times(1))
                .findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(participantCohortStatusDao, times(1))
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReview.getCohortReviewId(), participantId);
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    @Test
    public void updateParticipantCohortStatus() {
        String workspaceNamespace = "aou-test";
        String workspaceName = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long participantId = 1;
        long workspaceId = 1;
        ModifyCohortStatusRequest cohortStatusRequest = new ModifyCohortStatusRequest();
        cohortStatusRequest.setStatus(CohortStatus.INCLUDED);

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(workspaceNamespace);
        workspace.setFirecloudName(workspaceName);

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortReviewId(1);

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey();
        key.setParticipantId(participantId);
        ParticipantCohortStatus participantCohortStatus = new ParticipantCohortStatus();
        participantCohortStatus.setStatus(CohortStatus.NOT_REVIEWED);
        participantCohortStatus.setParticipantKey(key);

        when(cohortDao.findOne(cohortId)).thenReturn(cohort);
        when(workspaceService.getRequired(workspaceNamespace, workspaceName)).thenReturn(workspace);
        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(participantCohortStatusDao
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReview.getCohortReviewId(), participantId)).thenReturn(participantCohortStatus);
        when(cohortReviewDao.save(isA(CohortReview.class))).thenReturn(cohortReview);

        org.pmiops.workbench.model.ParticipantCohortStatus expectedPcs =
                new org.pmiops.workbench.model.ParticipantCohortStatus();
        expectedPcs.setParticipantId(participantId);
        expectedPcs.setStatus(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.ParticipantCohortStatus pcs = reviewController.updateParticipantCohortStatus(
                workspaceNamespace,
                workspaceName,
                cohortId,
                cdrVersionId,
                participantId,
                cohortStatusRequest).getBody();
        assertEquals(expectedPcs, pcs);

        verify(cohortDao, times(1)).findOne(cohortId);
        verify(workspaceService, times(1)).getRequired(workspaceNamespace, workspaceName);
        verify(cohortReviewDao, times(1))
                .findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(participantCohortStatusDao, times(1))
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReview.getCohortReviewId(), participantId);
        verify(cohortReviewDao, times(1)).save(isA(CohortReview.class));
        verifyNoMoreInteractions(cohortReviewDao, bigQueryService, workspaceService, participantCounter);
    }

    private void assertFindByCohortIdAndCdrVersionId(String namespace,
                                                     String name,
                                                     long cohortId,
                                                     long cdrVersionId,
                                                     Integer page,
                                                     Integer limit,
                                                     String order,
                                                     String column) {
        Integer pageParam = page == null ? 0 : page;
        Integer limitParam = limit == null ? 25 : limit;
        Sort.Direction orderParam = (order == null || order.equals("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String columnParam = (column == null || column.equals("participantId")) ? "participantKey.participantId" : column;

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortReviewId(cohortId).participantId(1L);
        ParticipantCohortStatus dbParticipant = new ParticipantCohortStatus().participantKey(key).status(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.ParticipantCohortStatus respParticipant =
                new org.pmiops.workbench.model.ParticipantCohortStatus()
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED);
        org.pmiops.workbench.model.CohortReview respCohortReview =
                new org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(1L)
                        .cohortId(cohortId)
                        .cdrVersionId(cdrVersionId)
                        .matchedParticipantCount(1000L)
                        .reviewedCount(0L)
                        .reviewSize(200L)
                .participantCohortStatuses(Arrays.asList(respParticipant));

        List<ParticipantCohortStatus> participants = new ArrayList<ParticipantCohortStatus>();
        participants.add(dbParticipant);
        Page expectedPage = new PageImpl(participants);

        CohortReview cohortReviewAfter = new CohortReview();
        cohortReviewAfter.setCohortReviewId(1L);
        cohortReviewAfter.setCohortId(cohortId);
        cohortReviewAfter.setCdrVersionId(cdrVersionId);
        cohortReviewAfter.setMatchedParticipantCount(1000);
        cohortReviewAfter.setReviewSize(200);
        cohortReviewAfter.setCreationTime(new Timestamp(System.currentTimeMillis()));

        final Sort sort = (columnParam.equals(CohortReviewController.PARTICIPANT_ID))
                ? new Sort(orderParam, columnParam)
                : new Sort(orderParam, columnParam, CohortReviewController.PARTICIPANT_ID);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReviewAfter);
        when(participantCohortStatusDao.findByParticipantKey_CohortReviewId(
                cohortId,
                new PageRequest(pageParam, limitParam, sort)))
                .thenReturn(expectedPage);

        ResponseEntity<org.pmiops.workbench.model.CohortReview> response =
                reviewController.getParticipantCohortStatuses(namespace, name, cohortId, cdrVersionId, page, limit, order, column);

        org.pmiops.workbench.model.CohortReview actualCohortReview = response.getBody();
        respCohortReview.setCreationTime(actualCohortReview.getCreationTime());
        assertEquals(respCohortReview, response.getBody());

        verify(cohortReviewDao, atLeast(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(participantCohortStatusDao, times(1))
                .findByParticipantKey_CohortReviewId(
                        cohortId,
                        new PageRequest(pageParam, limitParam, sort));
        verifyNoMoreInteractions(participantCohortStatusDao);
    }

}
