package org.pmiops.workbench.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.dao.ParticipantDao;
import org.pmiops.workbench.db.model.Participant;
import org.pmiops.workbench.db.model.ParticipantKey;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ParticipantListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CohortReviewControllerTest {

    @Mock
    ParticipantDao participantDao;

    @InjectMocks
    CohortReviewController reviewController = new CohortReviewController();

    @Test
    public void getParticipants() throws Exception {
        long cohortId = 1L;
        long cdrVersionId = 2L;
        int page = 1;
        int limit = 22;
        String order = "desc";
        String column = "status";

        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, null, null, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, page, null, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, null, limit, null, null);
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, null, null, order, null);
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, null, null, null, column);
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, page, limit, order, "participantId");
        assertFindByCohortIdAndCdrVersionId(cohortId, cdrVersionId, page, limit, order, column);
    }

    private void assertFindByCohortIdAndCdrVersionId(long cohortId, long cdrVersionId, Integer page, Integer limit, String order, String column) {
        Integer pageParam = page == null ? 0 : page;
        Integer limitParam = limit == null ? 25 : limit;
        Sort.Direction orderParam = (order == null || order.equals("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String columnParam = (column == null || column.equals("participantId")) ? "participantKey.participantId" : column;

        ParticipantKey key = new ParticipantKey().cohortId(cohortId).cdrVersionId(cdrVersionId).participantId(1L);
        Participant dbParticipant = new Participant().participantKey(key).status(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.Participant respParticipant =
                new org.pmiops.workbench.model.Participant()
                        .cohortId(cohortId)
                        .cdrVersionId(cdrVersionId)
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED);

        List<Participant> participants = new ArrayList<Participant>();
        participants.add(dbParticipant);
        Page expectedPage = new PageImpl(participants);

        when(participantDao.findParticipantByParticipantKey_CohortIdAndParticipantKey_CdrVersionId(
                cohortId, cdrVersionId,
                new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam))))
                .thenReturn(expectedPage);

        ResponseEntity<ParticipantListResponse> response =
                reviewController.getParticipants(cohortId, cdrVersionId, page, limit, order, column);

        assertEquals(1, response.getBody().getItems().size());
        assertEquals(respParticipant, response.getBody().getItems().get(0));

        verify(participantDao, times(1))
                .findParticipantByParticipantKey_CohortIdAndParticipantKey_CdrVersionId(
                        cohortId, cdrVersionId,
                        new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam)));
        verifyNoMoreInteractions(participantDao);
    }

}
