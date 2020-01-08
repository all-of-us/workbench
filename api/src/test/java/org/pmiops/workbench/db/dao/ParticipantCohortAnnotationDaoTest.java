package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ParticipantCohortAnnotationDaoTest {

  private static long COHORT_ID = 1;
  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private DbParticipantCohortAnnotation pca;
  private DbParticipantCohortAnnotation pca1;
  private long cohortReviewId = 3L;
  private long participantId = 4L;

  @Before
  public void setUp() {
    DbCohortAnnotationDefinition enumAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(COHORT_ID)
            .columnName("enum")
            .annotationTypeEnum(AnnotationType.ENUM);
    DbCohortAnnotationEnumValue enumValue1 =
        new DbCohortAnnotationEnumValue()
            .name("z")
            .order(0)
            .cohortAnnotationDefinition(enumAnnotationDefinition);
    DbCohortAnnotationEnumValue enumValue2 =
        new DbCohortAnnotationEnumValue()
            .name("r")
            .order(1)
            .cohortAnnotationDefinition(enumAnnotationDefinition);
    DbCohortAnnotationEnumValue enumValue3 =
        new DbCohortAnnotationEnumValue()
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
        new DbParticipantCohortAnnotation()
            .cohortAnnotationDefinitionId(
                booleanAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortReviewId(cohortReviewId)
            .participantId(participantId)
            .annotationValueBoolean(Boolean.TRUE);
    pca1 =
        new DbParticipantCohortAnnotation()
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
  public void save() {
    assertEquals(pca, participantCohortAnnotationDao.findOne(pca.getAnnotationId()));
  }

  @Test
  public void findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId() {
    assertEquals(
        pca,
        participantCohortAnnotationDao
            .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                cohortReviewId, pca.getCohortAnnotationDefinitionId(), participantId));
  }

  @Test
  public void findByAnnotationIdAndCohortReviewIdAndParticipantId() {
    assertEquals(
        pca,
        participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
            pca.getAnnotationId(), cohortReviewId, participantId));
  }

  @Test
  public void findByCohortReviewIdAndParticipantId() {
    List<DbParticipantCohortAnnotation> annotations =
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
