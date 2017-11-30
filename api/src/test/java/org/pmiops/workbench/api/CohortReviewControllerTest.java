package org.pmiops.workbench.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cohortbuilder.SubjectCounter;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.CohortDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ParticipantCohortStatusListResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
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
    SubjectCounter subjectCounter;

    @InjectMocks
    CohortReviewController reviewController;

    @Test
    public void getCohortReviewInfo_ReviewExists() throws Exception {
        long cohortId = 1;
        long cdrVersionId = 1;

        CohortReview cohortReview = new CohortReview().cohortReviewId(1).matchedParticipantCount(1000);

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(cohortReview);

        reviewController.getCohortReviewInfo(cohortId, cdrVersionId);

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, subjectCounter);
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

        when(cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId)).thenReturn(null);
        when(cohortDao.findCohortByCohortId(cohortId)).thenReturn(definition);
        when(subjectCounter.buildSubjectCounterQuery(request)).thenReturn(null);
        when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
        when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
        when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
        when(queryResult.iterateAll()).thenReturn(testIterable);
        when(bigQueryService.getLong(null, 0)).thenReturn(1000L);

        reviewController.getCohortReviewInfo(cohortId, cdrVersionId);

        verify(cohortReviewDao, times(1)).findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);
        verify(cohortDao, times(1)).findCohortByCohortId(cohortId);
        verify(subjectCounter, times(1)).buildSubjectCounterQuery(request);
        verify(bigQueryService, times(1)).filterBigQueryConfig(null);
        verify(bigQueryService, times(1)).executeQuery(null);
        verify(bigQueryService, times(1)).getResultMapper(queryResult);
        verify(bigQueryService, times(1)).getLong(null, 0);
        verify(queryResult, times(1)).iterateAll();
        verifyNoMoreInteractions(cohortReviewDao, cohortDao, bigQueryService, subjectCounter);
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
