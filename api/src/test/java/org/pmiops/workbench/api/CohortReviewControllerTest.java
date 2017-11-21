package org.pmiops.workbench.api;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ParticipantCohortStatusListResponse;
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
    ParticipantCohortStatusDao participantCohortStatusDao;

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

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey().cohortId(cohortId).cdrVersionId(cdrVersionId).participantId(1L);
        ParticipantCohortStatus dbParticipant = new ParticipantCohortStatus().participantKey(key).status(CohortStatus.INCLUDED);

        org.pmiops.workbench.model.ParticipantCohortStatus respParticipant =
                new org.pmiops.workbench.model.ParticipantCohortStatus()
                        .participantId(1L)
                        .status(CohortStatus.INCLUDED);

        List<ParticipantCohortStatus> participants = new ArrayList<ParticipantCohortStatus>();
        participants.add(dbParticipant);
        Page expectedPage = new PageImpl(participants);

        when(participantCohortStatusDao.findParticipantByParticipantKey_CohortIdAndParticipantKey_CdrVersionId(
                cohortId, cdrVersionId,
                new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam))))
                .thenReturn(expectedPage);

        ResponseEntity<ParticipantCohortStatusListResponse> response =
                reviewController.getParticipantCohortStatuses(cohortId, cdrVersionId, page, limit, order, column);

        assertEquals(1, response.getBody().getItems().size());
        assertEquals(respParticipant, response.getBody().getItems().get(0));

        verify(participantCohortStatusDao, times(1))
                .findParticipantByParticipantKey_CohortIdAndParticipantKey_CdrVersionId(
                        cohortId, cdrVersionId,
                        new PageRequest(pageParam, limitParam, new Sort(orderParam, columnParam)));
        verifyNoMoreInteractions(participantCohortStatusDao);
    }

}
