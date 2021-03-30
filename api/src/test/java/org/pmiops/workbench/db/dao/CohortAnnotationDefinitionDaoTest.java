package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortAnnotationDefinitionDaoTest extends SpringTest {

  private static long COHORT_ID = 1;
  @Autowired CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private DbCohortAnnotationDefinition cohortAnnotationDefinition;

  @Before
  public void setUp() {
    cohortAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(createCohortAnnotationDefinition());
  }

  @Test
  public void saveNoEnumValues() {
    assertThat(
            cohortAnnotationDefinitionDao.findOne(
                cohortAnnotationDefinition.getCohortAnnotationDefinitionId()))
        .isEqualTo(cohortAnnotationDefinition);
  }

  @Test
  public void saveWithEnumValues() {
    DbCohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();
    DbCohortAnnotationEnumValue enumValue1 =
        new DbCohortAnnotationEnumValue()
            .name("z")
            .order(0)
            .cohortAnnotationDefinition(cohortAnnotationDefinition);
    DbCohortAnnotationEnumValue enumValue2 =
        new DbCohortAnnotationEnumValue()
            .name("r")
            .order(1)
            .cohortAnnotationDefinition(cohortAnnotationDefinition);
    DbCohortAnnotationEnumValue enumValue3 =
        new DbCohortAnnotationEnumValue()
            .name("a")
            .order(2)
            .cohortAnnotationDefinition(cohortAnnotationDefinition);
    cohortAnnotationDefinition.getEnumValues().add(enumValue1);
    cohortAnnotationDefinition.getEnumValues().add(enumValue2);
    cohortAnnotationDefinition.getEnumValues().add(enumValue3);

    cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

    DbCohortAnnotationDefinition cad =
        cohortAnnotationDefinitionDao.findOne(
            cohortAnnotationDefinition.getCohortAnnotationDefinitionId());
    assertThat(cohortAnnotationDefinition).isEqualTo(cad);
    assertThat(cohortAnnotationDefinition.getEnumValues()).isEqualTo(cad.getEnumValues());
  }

  @Test
  public void findByCohortIdAndColumnName() {
    assertThat(
            cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(
                cohortAnnotationDefinition.getCohortId(),
                cohortAnnotationDefinition.getColumnName()))
        .isEqualTo(cohortAnnotationDefinition);
  }

  @Test
  public void findByCohortIdOrderByEnumValuesAsc() {
    assertThat(
            cohortAnnotationDefinitionDao
                .findByCohortId(cohortAnnotationDefinition.getCohortId())
                .get(0))
        .isEqualTo(cohortAnnotationDefinition);
  }

  private DbCohortAnnotationDefinition createCohortAnnotationDefinition() {
    return new DbCohortAnnotationDefinition()
        .cohortId(COHORT_ID)
        .columnName("annotation name")
        .annotationTypeEnum(AnnotationType.BOOLEAN);
  }
}
