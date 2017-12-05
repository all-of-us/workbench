package org.pmiops.workbench.db.dao;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortReviewDaoTest {

    private static long COHORT_ID = 1;
    private static long CDR_VERSION_ID = 1;

    @Autowired
    CohortReviewDao cohortReviewDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void save() throws Exception {
        CohortReview cohortReview = createCohortReview();

        cohortReviewDao.save(cohortReview);

        String sql = "select count(*) from cohort_review where cohort_review_id = ?";
        final Object[] sqlParams = { cohortReview.getCohortReviewId() };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

    @Test
    public void update() throws Exception {
        CohortReview cohortReview = createCohortReview();

        cohortReviewDao.save(cohortReview);

        String sql = "select count(*) from cohort_review where cohort_review_id = ? and reviewed_count = ?";
        Object[] sqlParams = { cohortReview.getCohortReviewId(), cohortReview.getReviewedCount() };
        Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));

        cohortReview = cohortReviewDao.findOne(cohortReview.getCohortReviewId());
        cohortReview.setReviewedCount(3);
        cohortReviewDao.saveAndFlush(cohortReview);

        sql = "select count(*) from cohort_review where cohort_review_id = ? and reviewed_count = ?";
        sqlParams = new Object[] { cohortReview.getCohortReviewId(), cohortReview.getReviewedCount() };

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

    @Test
    public void findCohortReviewByCohortIdAndCdrVersionId() throws Exception {
        CohortReview cohortReview = createCohortReview();

        cohortReviewDao.save(cohortReview);

        assertEquals(cohortReview, cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortReview.getCohortId(),
                cohortReview.getCdrVersionId()));
    }

    private CohortReview createCohortReview() {
        final Sort sort = new Sort(Sort.Direction.ASC, "status");
        final PageRequest pageRequest = new PageRequest(0, 25, sort);

        Gson gson = new Gson();

        return new CohortReview()
                .cohortId(COHORT_ID)
                .cdrVersionId(CDR_VERSION_ID)
                .creationTime(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .lastModifiedTime(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .matchedParticipantCount(100)
                .reviewedCount(10);
    }

}
