package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({LiquibaseAutoConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortAnnotationDefinitionDaoTest {

  private static long COHORT_ID = 1;
  @Autowired CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  private DbCohortAnnotationDefinition cohortAnnotationDefinition;

  @Before
  public void setUp() {
    cohortAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(createCohortAnnotationDefinition());
  }

  @Test
  public void saveNoEnumValues() throws Exception {
    assertEquals(
        cohortAnnotationDefinition,
        cohortAnnotationDefinitionDao.findOne(
            cohortAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @Test
  public void saveWithEnumValues() throws Exception {
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
    assertEquals(cohortAnnotationDefinition, cad);
    assertEquals(cohortAnnotationDefinition.getEnumValues(), cad.getEnumValues());
  }

  @Test
  public void findByCohortIdAndColumnName() throws Exception {
    assertEquals(
        cohortAnnotationDefinition,
        cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(
            cohortAnnotationDefinition.getCohortId(), cohortAnnotationDefinition.getColumnName()));
  }

  @Test
  public void findByCohortIdOrderByEnumValuesAsc() throws Exception {
    assertEquals(
        cohortAnnotationDefinition,
        cohortAnnotationDefinitionDao
            .findByCohortId(cohortAnnotationDefinition.getCohortId())
            .get(0));
  }

  private DbCohortAnnotationDefinition createCohortAnnotationDefinition() {
    return new DbCohortAnnotationDefinition()
        .cohortId(COHORT_ID)
        .columnName("annotation name")
        .annotationTypeEnum(AnnotationType.BOOLEAN);
  }
}
