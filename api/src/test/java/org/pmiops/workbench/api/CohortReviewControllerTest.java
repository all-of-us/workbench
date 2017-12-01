package org.pmiops.workbench.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import org.pmiops.workbench.db.model.CohortDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ParticipantCohortStatusListResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    @InjectMocks
    CohortReviewController reviewController;

    @Test
    public void getCohortReviewInfo_ReviewExists() throws Exception {
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview()
                .cohortReviewId(1)
                .matchedParticipantCount(1000)
                .creationTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);

        reviewController.getCohortReviewInfo(cohortId, cdrVersionId);

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void getCohortReviewInfo_ReviewNotExists() throws Exception {
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortDefinition definition = new CohortDefinition() {
            @Override
            public String getCriteria() {
                return "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                        "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                        "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";
            }
        };

        SearchRequest request = new Gson().fromJson(definition.getCriteria(), SearchRequest.class);
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
        rm.put("count", 0);

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortId(cohortId);
        cohortReview.setCdrVersionId(cdrVersionId);
        cohortReview.setMatchedParticipantCount(1000);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);
        when(cohortReviewDao.save(cohortReview)).thenReturn(cohortReview);
        when(cohortDao.findCohortByCohortId(cohortId)).thenReturn(definition);
        when(participantCounter.buildParticipantCounterQuery(request)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(1000L);

        reviewController.getCohortReviewInfo(cohortId, cdrVersionId);

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortReviewDao, times(1)).save(isA(CohortReview.class));
        verify(cohortDao, times(1)).findCohortByCohortId(cohortId);
        verify(participantCounter, times(1)).buildParticipantCounterQuery(request);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void getCohortReviewInfo_BadRequest() throws Exception {
        long cohortId = 1;
        long cdrVersionId = 1;

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);
        when(cohortDao.findCohortByCohortId(cohortId)).thenReturn(null);

        try {
            reviewController.getCohortReviewInfo(cohortId, cdrVersionId);
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort definition matching cohortId: 1", e.getMessage());
        }

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findCohortByCohortId(cohortId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview_ReviewAlreadyCreated() throws Exception {
        long cohortReviewId = 1;

        when(participantCohortStatusDao.countByParticipantKey_CohortReviewId(cohortReviewId)).thenReturn(200L);

        try {
            reviewController.createCohortReview(cohortReviewId, 200);
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: Cohort Review already created for cohortReviewId: 1", e.getMessage());
        }

        verify(participantCohortStatusDao, times(1)).countByParticipantKey_CohortReviewId(cohortReviewId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview_BadCohortReviewId() throws Exception {
        long cohortReviewId = 1;

        when(participantCohortStatusDao.countByParticipantKey_CohortReviewId(cohortReviewId)).thenReturn(0L);
        when(cohortReviewDao.findOne(cohortReviewId)).thenReturn(null);

        try {
            reviewController.createCohortReview(cohortReviewId, 200);
        } catch (BadRequestException e) {
            assertEquals("Invalid Request: No Cohort Review matching cohortReviewId: 1", e.getMessage());
        }

        verify(participantCohortStatusDao, times(1)).countByParticipantKey_CohortReviewId(cohortReviewId);
        verify(cohortReviewDao, times(1)).findOne(cohortReviewId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void createCohortReview() throws Exception {
        long cohortReviewId = 1;
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview();
        cohortReview.setCohortReviewId(cohortReviewId);
        cohortReview.setCohortId(cohortId);
        cohortReview.setCdrVersionId(cdrVersionId);
        cohortReview.setMatchedParticipantCount(1000);

        CohortDefinition definition = new CohortDefinition() {
            @Override
            public String getCriteria() {
                return "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                        "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                        "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";
            }
        };

        SearchRequest request = new Gson().fromJson(definition.getCriteria(), SearchRequest.class);
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

        when(participantCohortStatusDao.countByParticipantKey_CohortReviewId(cohortReviewId)).thenReturn(0L);
        when(cohortReviewDao.findOne(cohortReviewId)).thenReturn(cohortReview);
        when(cohortDao.findCohortByCohortId(cohortId)).thenReturn(definition);
        when(participantCounter.buildParticipantIdQuery(request, 200)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(0L);

        reviewController.createCohortReview(cohortReviewId, 200);

        verify(participantCohortStatusDao, times(1)).countByParticipantKey_CohortReviewId(cohortReviewId);
        verify(cohortReviewDao, times(1)).findOne(cohortReviewId);
        verify(cohortDao, times(1)).findCohortByCohortId(cohortId);
        verify(participantCounter, times(1)).buildParticipantIdQuery(request, 200);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, participantCounter);
    }

    @Test
    public void getParticipants() throws Exception {
        long cohortReviewId = 1L;
        int page = 1;
        int limit = 22;
        String order = "desc";
        String column = "status";

        assertFindByCohortIdAndCdrVersionId(cohortReviewId, null, null, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, page, null, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, null, limit, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, null, null, order, null);
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, null, null, null, column);
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, page, limit, order, "participantId");
        assertFindByCohortIdAndCdrVersionId(cohortReviewId, page, limit, order, column);
    }

    private void assertFindByCohortIdAndCdrVersionId(long cohortReviewId, Integer page, Integer limit, String order, String column) {
        Integer pageParam = page == null ? 0 : page;
        Integer limitParam = limit == null ? 25 : limit;
        Sort.Direction orderParam = (order == null || order.equals("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String columnParam = (column == null || column.equals("participantId")) ? "participantKey.participantId" : column;

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortReviewId(cohortReviewId).participantId(1L);
        ParticipantCohortStatus dbParticipant = new ParticipantCohortStatus().participantKey(key).status(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.ParticipantCohortStatus respParticipant =
                new org.pmiops.workbench.model.ParticipantCohortStatus()
                        .cohortReviewId(cohortReviewId)
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED);

        List<ParticipantCohortStatus> participants = new ArrayList<ParticipantCohortStatus>();
        participants.add(dbParticipant);
        Page expectedPage = new PageImpl(participants);

        when(participantCohortStatusDao.findParticipantByParticipantKey_CohortReviewId(
                cohortReviewId,
                new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam))))
                .thenReturn(expectedPage);

        ResponseEntity<ParticipantCohortStatusListResponse> response =
                reviewController.getParticipantCohortStatuses(cohortReviewId, page, limit, order, column);

        assertEquals(1, response.getBody().getItems().size());
        assertEquals(respParticipant, response.getBody().getItems().get(0));

        verify(participantCohortStatusDao, times(1))
                .findParticipantByParticipantKey_CohortReviewId(
                        cohortReviewId,
                        new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam)));
        verifyNoMoreInteractions(participantCohortStatusDao);
    }

}
