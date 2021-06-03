package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import({CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ParticipantCohortAnnotationDaoTest extends SpringTest {

  private static long COHORT_ID = 1;
  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private DbParticipantCohortAnnotation pca;
  private DbParticipantCohortAnnotation pca1;
  private long cohortReviewId = 3L;
  private long participantId = 4L;

  @BeforeEach
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
    enumAnnotationDefinition = cohortAnnotationDefinitionDao.save(enumAnnotationDefinition);

    DbCohortAnnotationDefinition booleanAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(COHORT_ID)
            .columnName("boolean")
            .annotationTypeEnum(AnnotationType.BOOLEAN);
    booleanAnnotationDefinition = cohortAnnotationDefinitionDao.save(booleanAnnotationDefinition);

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
    pca = participantCohortAnnotationDao.save(pca);
    pca1 = participantCohortAnnotationDao.save(pca1);
  }

  @Test
  public void save() {
    assertThat(participantCohortAnnotationDao.findById(pca.getAnnotationId()).get()).isEqualTo(pca);
  }

  @Test
  public void findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId() {
    assertThat(
            participantCohortAnnotationDao
                .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                    cohortReviewId, pca.getCohortAnnotationDefinitionId(), participantId))
        .isEqualTo(pca);
  }

  @Test
  public void findByAnnotationIdAndCohortReviewIdAndParticipantId() {
    assertThat(
            participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                pca.getAnnotationId(), cohortReviewId, participantId))
        .isEqualTo(pca);
  }

  @Test
  public void findByCohortReviewIdAndParticipantId() {
    List<DbParticipantCohortAnnotation> annotations =
        participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(
            cohortReviewId, participantId);
    assertThat(annotations.size()).isEqualTo(2);
    assertThat(annotations.get(0)).isEqualTo(pca);
    assertThat(annotations.get(1)).isEqualTo(pca1);
    assertThat(
            annotations
                .get(1)
                .getCohortAnnotationEnumValue()
                .getCohortAnnotationDefinition()
                .getEnumValues())
        .isEqualTo(
            pca1.getCohortAnnotationEnumValue().getCohortAnnotationDefinition().getEnumValues());
  }
}
