package org.pmiops.workbench.cohortreview;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.testconfig.TestWorkbenchJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Date;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestWorkbenchJpaConfig.class})
@ActiveProfiles("test-workbench")
public class CohortReviewServiceImplTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void saveParticipantCohortStatuses() throws Exception {
        ParticipantCohortStatusKey key1 = new ParticipantCohortStatusKey(1L, 1L);
        ParticipantCohortStatusKey key2 = new ParticipantCohortStatusKey(1L, 2L);

        ParticipantCohortStatus pcs1 = createParticipantCohortStatus(
                key1,
                1L,
                2L,
                3L);
        ParticipantCohortStatus pcs2 = createParticipantCohortStatus(
                key2,
                1L,
                2L,
                3L);

        CohortReviewServiceImpl cohortReviewService = new CohortReviewServiceImpl(null,
                null,
                null,
                null,
                jdbcTemplate);
        cohortReviewService.saveParticipantCohortStatuses(Arrays.asList(pcs1, pcs2));

        String sql = "select count(*) from participant_cohort_status where cohort_review_id = ? " +
                "and participant_id = ? and ethnicity_concept_id = ? and gender_concept_id = ? " +
                "and race_concept_id = ?";
        final Object[] sqlParams1 = { 1, 1, 1, 2, 3 };
        final Object[] sqlParams2 = { 1, 2, 1, 2, 3 };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams1, Integer.class));
        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams2, Integer.class));
    }

    private ParticipantCohortStatus createParticipantCohortStatus(ParticipantCohortStatusKey key,
                                                 Long ethnicityConceptId,
                                                 Long genderConceptId,
                                                 Long raceConceptId) {
        return new ParticipantCohortStatus()
                .birthDate(new Date(System.currentTimeMillis()))
                .ethnicityConceptId(ethnicityConceptId)
                .genderConceptId(genderConceptId)
                .raceConceptId(raceConceptId)
                .status(CohortStatus.INCLUDED)
                .participantKey(key);
    }

}