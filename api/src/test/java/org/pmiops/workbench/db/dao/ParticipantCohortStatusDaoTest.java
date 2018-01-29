package org.pmiops.workbench.db.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.pmiops.workbench.cohortreview.util.SortColumn;
import org.pmiops.workbench.cohortreview.util.SortOrder;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.testconfig.TestCdrJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestCdrJpaConfig.class})
@ActiveProfiles("test-cdr")
@DataJpaTest
@Import({LiquibaseAutoConfiguration.class})
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ParticipantCohortStatusDaoTest {
    private static Long COHORT_REVIEW_ID = 1L;

    @Autowired
    ParticipantCohortStatusDao participantCohortStatusDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Before
    public void onSetup() {
        jdbcTemplate.execute("insert into participant_cohort_status" +
                "(cohort_review_id, participant_id, status, gender_concept_id, birth_date, race_concept_id, ethnicity_concept_id)" +
                "values (1, 1, 1, 1, sysdate(), 2, 3)");
        jdbcTemplate.execute("insert into participant_cohort_status" +
                "(cohort_review_id, participant_id, status, gender_concept_id, birth_date, race_concept_id, ethnicity_concept_id)" +
                "values (1, 2, 0, 1, sysdate(), 2, 3)");
        jdbcTemplate.execute("insert into cdr.concept" +
                "(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence)" +
                "values (1, 'MALE', 3, 'Gender', 1, 'c', 'c', 1, 1)");
        jdbcTemplate.execute("insert into cdr.concept" +
                "(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence)" +
                "values (2, 'Asian', 3, 'Race', 1, 'c', 'c', 1, 1)");
        jdbcTemplate.execute("insert into cdr.concept" +
                "(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence)" +
                "values (3, 'Not Hispanic', 3, 'Ethnicity', 1, 'c', 'c', 1, 1)");
    }

    @After
    public void onTearDown() {
        jdbcTemplate.execute("delete from participant_cohort_status");
        jdbcTemplate.execute("delete from cdr.concept");
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId_Paging() throws Exception {

        final Sort sort = new Sort(Sort.Direction.ASC, "participantKey.participantId");
        assertParticipant(new PageRequest(0, 1, sort),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED));
        assertParticipant(new PageRequest(1, 1, sort),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),CohortStatus.EXCLUDED));
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId_Sorting() throws Exception {

        final Sort sortParticipantAsc = new Sort(Sort.Direction.ASC, "participantKey.participantId");
        final Sort sortParticipantDesc = new Sort(Sort.Direction.DESC, "participantKey.participantId");
        final Sort sortStatusAsc = new Sort(Sort.Direction.ASC, "status");
        final Sort sortStatusDesc = new Sort(Sort.Direction.DESC, "status");

        assertParticipant(new PageRequest(0, 1, sortParticipantAsc),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED));
        assertParticipant(new PageRequest(0, 1, sortParticipantDesc),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),CohortStatus.EXCLUDED));
        assertParticipant(new PageRequest(0, 1, sortStatusAsc),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),CohortStatus.EXCLUDED));
        assertParticipant(new PageRequest(0, 1, sortStatusDesc),
                createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED));
    }

    @Test
    public void findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId() throws Exception {
        ParticipantCohortStatus participant1 = createExpectedPCS(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED);
        ParticipantCohortStatus actualParticipant = participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                COHORT_REVIEW_ID,
                participant1.getParticipantKey().getParticipantId()
        );
        participant1.setBirthDate(actualParticipant.getBirthDate());
        assertEquals(participant1, actualParticipant);
    }

    /**
     * Prefixed this test with a a z_ to ensure that it runs last using the {@link FixMethodOrder} annotation.
     * This method has to run last because the batching implementation has to call a commit. This commit causes
     * problems with other test cases results.
     *
     * @throws Exception
     */
    @Test
    public void z_saveParticipantCohortStatuses() throws Exception {
        ParticipantCohortStatusKey key1 = new ParticipantCohortStatusKey().cohortReviewId(2).participantId(3);
        ParticipantCohortStatusKey key2 = new ParticipantCohortStatusKey().cohortReviewId(2).participantId(4);
        ParticipantCohortStatus pcs1 = new ParticipantCohortStatus()
                .participantKey(key1)
                .status(CohortStatus.INCLUDED)
                .birthDate(new Date(System.currentTimeMillis()))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);
        ParticipantCohortStatus pcs2 = new ParticipantCohortStatus()
                .participantKey(key2)
                .status(CohortStatus.EXCLUDED)
                .birthDate(new Date(System.currentTimeMillis()))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L);

        participantCohortStatusDao.saveParticipantCohortStatusesCustom(Arrays.asList(pcs1, pcs2));
        List<ParticipantCohortStatus> pcdList =
                participantCohortStatusDao.findByParticipantKey_CohortReviewId(2, new PageRequest(0, 10)).getContent();
        assertEquals(2, pcdList.size());

        participantCohortStatusDao.delete(pcs1);
        participantCohortStatusDao.delete(pcs2);
    }

    @Test
    public void findAll_NoSearchCriteria() throws Exception {
        int page = 0;
        int pageSize = 25;
        org.pmiops.workbench.cohortreview.util.PageRequest pageRequest =
                new org.pmiops.workbench.cohortreview.util.PageRequest(0, 25, SortOrder.asc, SortColumn.PARTICIPANT_ID);
        List<ParticipantCohortStatus> results = participantCohortStatusDao.findAll(null, pageRequest);

        assertEquals(2, results.size());

        ParticipantCohortStatus participant1 = createExpectedPCSWithConceptValues(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED);
        participant1.setBirthDate(results.get(0).getBirthDate());
        ParticipantCohortStatus participant2 = createExpectedPCSWithConceptValues(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.EXCLUDED);
        participant2.setBirthDate(results.get(1).getBirthDate());

        assertEquals(participant1, results.get(0));
        assertEquals(participant2, results.get(1));
    }

    @Test
    public void findAll_SearchCriteria() throws Exception {
        int page = 0;
        int pageSize = 25;
        org.pmiops.workbench.cohortreview.util.PageRequest pageRequest =
                new org.pmiops.workbench.cohortreview.util.PageRequest(0, 25, SortOrder.asc, SortColumn.PARTICIPANT_ID);
        List<ParticipantCohortStatus> results = participantCohortStatusDao.findAll(null, pageRequest);

        assertEquals(2, results.size());

        ParticipantCohortStatus participant1 = createExpectedPCSWithConceptValues(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.INCLUDED);
        participant1.setBirthDate(results.get(0).getBirthDate());
        ParticipantCohortStatus participant2 = createExpectedPCSWithConceptValues(new ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),CohortStatus.EXCLUDED);
        participant2.setBirthDate(results.get(1).getBirthDate());

        assertEquals(participant1, results.get(0));
        assertEquals(participant2, results.get(1));
    }

    private void assertParticipant(Pageable pageRequest, ParticipantCohortStatus expectedParticipant) {
        Slice<ParticipantCohortStatus> participants = participantCohortStatusDao
                .findByParticipantKey_CohortReviewId(
                        expectedParticipant.getParticipantKey().getCohortReviewId(),
                        pageRequest);
        expectedParticipant.setBirthDate(participants.getContent().get(0).getBirthDate());
        assertEquals(expectedParticipant, participants.getContent().get(0));
    }

    private ParticipantCohortStatus createExpectedPCS(ParticipantCohortStatusKey key, CohortStatus status) {
        return new ParticipantCohortStatus()
                .participantKey(key)
                .status(status)
                .ethnicityConceptId(3L)
                .genderConceptId(1L)
                .raceConceptId(2L);
    }

    private ParticipantCohortStatus createExpectedPCSWithConceptValues(ParticipantCohortStatusKey key, CohortStatus status) {
        return new ParticipantCohortStatus()
                .participantKey(key)
                .status(status)
                .ethnicityConceptId(3L)
                .ethnicity("Not Hispanic")
                .genderConceptId(1L)
                .gender("MALE")
                .raceConceptId(2L)
                .race("Asian");
    }

}
