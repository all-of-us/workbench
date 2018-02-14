package org.pmiops.workbench.db.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ParticipantCohortAnnotationDaoTest {

    @Autowired
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long annotationId;

    @Before
    public void setUp() throws Exception {
        ParticipantCohortAnnotation pca = new ParticipantCohortAnnotation()
                .annotationId(1L)
                .cohortAnnotationDefinitionId(1L)
                .cohortReviewId(1L)
                .participantId(1L)
                .annotationValueBoolean(Boolean.TRUE);
        annotationId = participantCohortAnnotationDao.save(pca).getAnnotationId();
    }

    @Test
    public void save() throws Exception {
        String sql = "select count(*) from participant_cohort_annotations where annotation_id = ?";
        final Object[] sqlParams = { annotationId };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

}
