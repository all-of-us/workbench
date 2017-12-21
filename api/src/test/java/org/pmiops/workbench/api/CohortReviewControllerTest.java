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
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
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
    CohortReviewService cohortReviewService;

    @Mock
    BigQueryService bigQueryService;

    @Mock
    ParticipantCounter participantCounter;

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

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(cohortReview);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review already created for cohortId: "
                    + cohortId + ", cdrVersionId: " + cdrVersionId, e.getMessage());
        }

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verifyNoMoreMockInteractions();
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

        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortReview_NoCohortDefinitionFound() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long workspaceId = 1;

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortId(cohortId);
        Cohort cohort = new Cohort();
        cohort.setCohortId(cohortId);
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
        workspace.setWorkspaceNamespace(namespace);
        workspace.setFirecloudName(name);

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortReviewService.findCohort(cohortId)).thenReturn(cohort);
        doNothing().when(cohortReviewService).validateMatchingWorkspace(namespace, name, workspaceId);

        try {
            reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));
            fail("Should have thrown a NotFoundException!");
        } catch (NotFoundException e) {
            assertEquals("Not Found: No Cohort definition matching cohortId: "
                         + cohortId,
                         e.getMessage());
        }

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1)).findCohort(cohortId);
        verify(cohortReviewService, times(1)).validateMatchingWorkspace(namespace, name, workspaceId);
        verifyNoMoreMockInteractions();
    }

    @Test
    public void createCohortReview() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1;
        long cdrVersionId = 1;
        long cohortReviewId = 1;
        long workspaceId = 1;

        CohortReview cohortReview = new CohortReview();
        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(workspaceId);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);
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

        SearchRequest searchRequest = new Gson().fromJson(definition, SearchRequest.class);

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

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(cohortReview);
        when(cohortReviewService.findCohort(cohortId)).thenReturn(cohort);
        doNothing().when(cohortReviewService).validateMatchingWorkspace(namespace, name, workspaceId);
        doNothing().when(codeDomainLookupService).findCodesForEmptyDomains(searchRequest.getIncludes());
        doNothing().when(codeDomainLookupService).findCodesForEmptyDomains(searchRequest.getExcludes());
        when(participantCounter.buildParticipantIdQuery(request, 200, 0L)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(0L);
        when(cohortReviewService.saveCohortReview(cohortReviewAfter)).thenReturn(cohortReviewAfter);
        when(cohortReviewService.saveParticipantCohortStatuses(isA(List.class))).thenReturn(new ArrayList<>());

        reviewController.createCohortReview(namespace, name, cohortId, cdrVersionId, new CreateReviewRequest().size(200));

        verify(cohortReviewService, times(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1)).findCohort(cohortId);
        verify(cohortReviewService, times(1)).validateMatchingWorkspace(namespace, name, workspaceId);
        verify(codeDomainLookupService, times(1)).findCodesForEmptyDomains(searchRequest.getIncludes());
        verify(codeDomainLookupService, times(1)).findCodesForEmptyDomains(searchRequest.getExcludes());
        verify(participantCounter, times(1)).buildParticipantIdQuery(request, 200, 0L);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verify(cohortReviewService, times(1)).saveCohortReview(cohortReviewAfter);
        verify(cohortReviewService, times(1)).saveParticipantCohortStatuses(isA(List.class));
        verifyNoMoreMockInteractions();
    }

    @Test
    public void getParticipants() throws Exception {
        String namespace = "aou-test";
        String name = "test";
        long cohortId = 1L;
        long cdrVersionId = 1L;
        int page = 1;
        int pageSize = 22;
        String sortOrder = "desc";
        String sortColumn = "status";

        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, pageSize, sortOrder, sortColumn);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page, pageSize, sortOrder,"participantId");
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null,null,null, sortColumn);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null,null, sortOrder,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, pageSize,null,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, page,null,null,null);
        assertFindByCohortIdAndCdrVersionId(namespace, name, cohortId, cdrVersionId, null, null,null,null);
    }

    private void assertFindByCohortIdAndCdrVersionId(String namespace,
                                                     String name,
                                                     long cohortId,
                                                     long cdrVersionId,
                                                     Integer page,
                                                     Integer pageSize,
                                                     String sortOrder,
                                                     String sortColumn) {
        Integer pageParam = page == null ? 0 : page;
        Integer pageSizeParam = pageSize == null ? 25 : pageSize;
        Sort.Direction orderParam = (sortOrder == null || sortOrder.equals("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String columnParam = (sortColumn == null || sortColumn.equals("participantId")) ? "participantKey.participantId" : sortColumn;

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
                        .page(pageParam)
                        .pageSize(pageSizeParam)
                        .sortOrder(orderParam.toString())
                        .sortColumn(columnParam)
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

        when(cohortReviewService.findCohortReview(cohortId, cdrVersionId)).thenReturn(cohortReviewAfter);
        when(cohortReviewService.findParticipantCohortStatuses(cohortId,
                new PageRequest(pageParam, pageSizeParam, sort))).thenReturn(expectedPage);

        ResponseEntity<org.pmiops.workbench.model.CohortReview> response =
                reviewController.getParticipantCohortStatuses(namespace, name, cohortId, cdrVersionId, page, pageSize, sortOrder, sortColumn);

        org.pmiops.workbench.model.CohortReview actualCohortReview = response.getBody();
        respCohortReview.setCreationTime(actualCohortReview.getCreationTime());
        assertEquals(respCohortReview, response.getBody());

        verify(cohortReviewService, atLeast(1)).findCohortReview(cohortId, cdrVersionId);
        verify(cohortReviewService, times(1))
                .findParticipantCohortStatuses(cohortId, new PageRequest(pageParam, pageSizeParam, sort));
        verifyNoMoreMockInteractions();
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(cohortReviewService, bigQueryService, participantCounter, codeDomainLookupService);
    }

}
