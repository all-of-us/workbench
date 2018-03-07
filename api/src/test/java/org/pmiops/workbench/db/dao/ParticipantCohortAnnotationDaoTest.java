package org.pmiops.workbench.db.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ParticipantCohortAnnotationDaoTest {

    private static long COHORT_ID = 1;

    @Autowired
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

    @Autowired
    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long annotationId;
    private long cohortAnnotationDefinitionId = 2L;
    private long cohortAnnotationDefinitionId1 = 3L;
    private long cohortReviewId = 3L;
    private long participantId = 4L;

    @Before
    public void setUp() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition =
                new CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("enum")
                .annotationType(AnnotationType.ENUM);
        CohortAnnotationEnumValue enumValue1 = new CohortAnnotationEnumValue().name("z").order(0).cohortAnnotationDefinition(cohortAnnotationDefinition);
        CohortAnnotationEnumValue enumValue2 = new CohortAnnotationEnumValue().name("r").order(1).cohortAnnotationDefinition(cohortAnnotationDefinition);
        CohortAnnotationEnumValue enumValue3 = new CohortAnnotationEnumValue().name("a").order(2).cohortAnnotationDefinition(cohortAnnotationDefinition);
        cohortAnnotationDefinition.getEnumValues().add(enumValue1);
        cohortAnnotationDefinition.getEnumValues().add(enumValue2);
        cohortAnnotationDefinition.getEnumValues().add(enumValue3);
        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        ParticipantCohortAnnotation pca = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueBoolean(Boolean.TRUE);
        ParticipantCohortAnnotation pca1 = new ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId1)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueEnum("test");
        pca1.setCohortAnnotationEnumValue(enumValue1);
        annotationId = participantCohortAnnotationDao.save(pca).getAnnotationId();
        participantCohortAnnotationDao.save(pca1);
    }

    @After
    public void onTearDown() {
        jdbcTemplate.execute("delete from participant_cohort_annotations");
    }

    @Test
    public void save() throws Exception {
        String sql = "select count(*) from participant_cohort_annotations where annotation_id = ?";
        final Object[] sqlParams = { annotationId };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

    @Test
    public void findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId() throws Exception {
        ParticipantCohortAnnotation expectedPCA = new ParticipantCohortAnnotation()
                .annotationId(annotationId)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueBoolean(Boolean.TRUE);
        ParticipantCohortAnnotation actualPCA =
                participantCohortAnnotationDao.findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                        cohortReviewId,
                        cohortAnnotationDefinitionId,
                        participantId);
        assertEquals(expectedPCA, actualPCA);
    }

    @Test
    public void findByAnnotationIdAndCohortReviewIdAndParticipantId() throws Exception {
        ParticipantCohortAnnotation expectedPCA = new ParticipantCohortAnnotation()
                .annotationId(annotationId)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueBoolean(Boolean.TRUE);
        ParticipantCohortAnnotation actualPCA =
        participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId,
                cohortReviewId,
                participantId);
        assertEquals(expectedPCA, actualPCA);
    }

    @Test
    public void findByCohortReviewIdAndParticipantId() throws Exception {
        ParticipantCohortAnnotation expectedPCA = new ParticipantCohortAnnotation()
            .annotationId(annotationId)
            .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId)
            .cohortReviewId(cohortReviewId)
            .participantId(participantId)
            .annotationValueBoolean(Boolean.TRUE);
        List<ParticipantCohortAnnotation> annotations =
        participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(cohortReviewId, participantId);
        assertEquals(2, annotations.size());
        assertEquals(expectedPCA, annotations.get(0));
    }

    @Test
    public void findByCohortReviewIdAndParticipantIdEnum() throws Exception {
        ParticipantCohortAnnotation expectedPCA = new ParticipantCohortAnnotation()
                .annotationId(annotationId)
                .cohortAnnotationDefinitionId(cohortAnnotationDefinitionId1)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueEnum("test");
        List<ParticipantCohortAnnotation> annotations =
                participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(cohortReviewId, participantId);
        assertEquals(2, annotations.size());
        assertEquals(expectedPCA, annotations.get(1));
        assertEquals(new CohortAnnotationEnumValue().name("z").order(0), annotations.get(1).getCohortAnnotationEnumValue());
    }

}
