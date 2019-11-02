package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ParticipantCohortAnnotationDaoTest {

  private static long COHORT_ID = 1;
  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private ParticipantCohortAnnotation pca;
  private ParticipantCohortAnnotation pca1;
  private long cohortReviewId = 3L;
  private long participantId = 4L;

  @Before
  public void setUp() throws Exception {
    DbCohortAnnotationDefinition enumAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(COHORT_ID)
            .columnName("enum")
            .annotationTypeEnum(AnnotationType.ENUM);
    CohortAnnotationEnumValue enumValue1 =
        new CohortAnnotationEnumValue()
            .name("z")
            .order(0)
            .cohortAnnotationDefinition(enumAnnotationDefinition);
    CohortAnnotationEnumValue enumValue2 =
        new CohortAnnotationEnumValue()
            .name("r")
            .order(1)
            .cohortAnnotationDefinition(enumAnnotationDefinition);
    CohortAnnotationEnumValue enumValue3 =
        new CohortAnnotationEnumValue()
            .name("a")
            .order(2)
            .cohortAnnotationDefinition(enumAnnotationDefinition);
    enumAnnotationDefinition.getEnumValues().add(enumValue1);
    enumAnnotationDefinition.getEnumValues().add(enumValue2);
    enumAnnotationDefinition.getEnumValues().add(enumValue3);
    cohortAnnotationDefinitionDao.save(enumAnnotationDefinition);

    DbCohortAnnotationDefinition booleanAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(COHORT_ID)
            .columnName("boolean")
            .annotationTypeEnum(AnnotationType.BOOLEAN);

    pca =
        new ParticipantCohortAnnotation()
            .cohortAnnotationDefinitionId(
                booleanAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortReviewId(cohortReviewId)
            .participantId(participantId)
            .annotationValueBoolean(Boolean.TRUE);
    pca1 =
        new ParticipantCohortAnnotation()
            .cohortAnnotationDefinitionId(
                enumAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortReviewId(cohortReviewId)
            .participantId(participantId)
            .annotationValueEnum("test");
    pca1.setCohortAnnotationEnumValue(enumValue1);
    participantCohortAnnotationDao.save(pca);
    participantCohortAnnotationDao.save(pca1);
  }

  @Test
  public void save() throws Exception {
    assertEquals(pca, participantCohortAnnotationDao.findOne(pca.getAnnotationId()));
  }

  @Test
  public void findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId()
      throws Exception {
    assertEquals(
        pca,
        participantCohortAnnotationDao
            .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                cohortReviewId, pca.getCohortAnnotationDefinitionId(), participantId));
  }

  @Test
  public void findByAnnotationIdAndCohortReviewIdAndParticipantId() throws Exception {
    assertEquals(
        pca,
        participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
            pca.getAnnotationId(), cohortReviewId, participantId));
  }

  @Test
  public void findByCohortReviewIdAndParticipantId() throws Exception {
    List<ParticipantCohortAnnotation> annotations =
        participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(
            cohortReviewId, participantId);
    assertEquals(2, annotations.size());
    assertEquals(pca, annotations.get(0));
    assertEquals(pca1, annotations.get(1));
    assertEquals(
        pca1.getCohortAnnotationEnumValue().getCohortAnnotationDefinition().getEnumValues(),
        annotations
            .get(1)
            .getCohortAnnotationEnumValue()
            .getCohortAnnotationDefinition()
            .getEnumValues());
  }
}
