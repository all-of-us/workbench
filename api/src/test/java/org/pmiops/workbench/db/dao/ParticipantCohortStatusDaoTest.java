package org.pmiops.workbench.db.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ParticipantCohortStatusDaoTest {

    private ParticipantCohortStatus participant1;
    private ParticipantCohortStatus participant2;
    private static Long COHORT_REVIEW_ID = 1L;

    @Autowired
    ParticipantCohortStatusDao participantCohortStatusDao;

    @Before
    public void onSetup() {
        ParticipantCohortStatusKey key1 = new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1);
        ParticipantCohortStatusKey key2 = new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2);
        participant1 = new ParticipantCohortStatus()
                .participantKey(key1)
                .status(CohortStatus.INCLUDED)
                .birthDate(new Date(-852058800000000L))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);
        participant2 = new ParticipantCohortStatus()
                .participantKey(key2)
                .status(CohortStatus.EXCLUDED)
                .birthDate(new Date(-852058800000000L))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);
        participantCohortStatusDao.save(Arrays.asList(participant1, participant2));
    }

    @After
    public void onTearDown() {
        participantCohortStatusDao.delete(participant1);
        participantCohortStatusDao.delete(participant2);
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId_Paging() throws Exception {

        final Sort sort = new Sort(Sort.Direction.ASC, "participantKey.participantId");
        assertParticipant(new PageRequest(0, 1, sort), participant1);
        assertParticipant(new PageRequest(1, 1, sort), participant2);
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId_Sorting() throws Exception {

        final Sort sortParticipantAsc = new Sort(Sort.Direction.ASC, "participantKey.participantId");
        final Sort sortParticipantDesc = new Sort(Sort.Direction.DESC, "participantKey.participantId");
        final Sort sortStatusAsc = new Sort(Sort.Direction.ASC, "status");
        final Sort sortStatusDesc = new Sort(Sort.Direction.DESC, "status");
        assertParticipant(new PageRequest(0, 1, sortParticipantAsc), participant1);
        assertParticipant(new PageRequest(0, 1, sortParticipantDesc), participant2);
        assertParticipant(new PageRequest(0, 1, sortStatusAsc), participant2);
        assertParticipant(new PageRequest(0, 1, sortStatusDesc), participant1);
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId() throws Exception {
        assertEquals(participant1,
                participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        COHORT_REVIEW_ID,
                        participant1.getParticipantKey().getParticipantId()
                ));
    }

    private void assertParticipant(Pageable pageRequest, ParticipantCohortStatus expectedParticipant) {
        Slice<ParticipantCohortStatus> participants = participantCohortStatusDao
                .findByParticipantKey_CohortReviewId(
                        expectedParticipant.getParticipantKey().getCohortReviewId(),
                        pageRequest);
        assertEquals(expectedParticipant, participants.getContent().get(0));
    }

}
