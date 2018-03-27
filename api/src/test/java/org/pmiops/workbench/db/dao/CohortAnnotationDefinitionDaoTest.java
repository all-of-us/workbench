package org.pmiops.workbench.db.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
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
@Import({LiquibaseAutoConfiguration.class})
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortAnnotationDefinitionDaoTest {

    private static long COHORT_ID = 1;

    @Autowired
    CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void save_NoEnumValues() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        String sql = "select count(*) from cohort_annotation_definition where cohort_annotation_definition_id = ?";
        final Object[] sqlParams = { cohortAnnotationDefinition.getCohortAnnotationDefinitionId() };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

    @Test
    public void save_WithEnumValues() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();
        CohortAnnotationEnumValue enumValue1 = new CohortAnnotationEnumValue().name("z").order(0).cohortAnnotationDefinition(cohortAnnotationDefinition);
        CohortAnnotationEnumValue enumValue2 = new CohortAnnotationEnumValue().name("r").order(1).cohortAnnotationDefinition(cohortAnnotationDefinition);
        CohortAnnotationEnumValue enumValue3 = new CohortAnnotationEnumValue().name("a").order(2).cohortAnnotationDefinition(cohortAnnotationDefinition);
        cohortAnnotationDefinition.getEnumValues().add(enumValue1);
        cohortAnnotationDefinition.getEnumValues().add(enumValue2);
        cohortAnnotationDefinition.getEnumValues().add(enumValue3);

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        String sql = "select count(*) from cohort_annotation_definition where cohort_annotation_definition_id = ?";
        Object[] sqlParams = { cohortAnnotationDefinition.getCohortAnnotationDefinitionId() };
        Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));

        sql = "select count(*) from cohort_annotation_enum_value where cohort_annotation_definition_id = ?";
        sqlParams = new Object[]{ cohortAnnotationDefinition.getCohortAnnotationDefinitionId() };
        expectedCount = new Integer("3");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));

        List<CohortAnnotationDefinition> cad =
                cohortAnnotationDefinitionDao.findByCohortId(cohortAnnotationDefinition.getCohortId());
        assertEquals(cohortAnnotationDefinition, cad.get(0));
    }

    @Test
    public void findByCohortIdAndColumnName() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        CohortAnnotationDefinition expectedAnnotationDefinition =
                cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(
                        cohortAnnotationDefinition.getCohortId(),
                        cohortAnnotationDefinition.getColumnName());

        assertEquals(expectedAnnotationDefinition, cohortAnnotationDefinition);
    }

    @Test
    public void findByCohortIdOrderByEnumValuesAsc() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        List<CohortAnnotationDefinition> expectedDBList =
                cohortAnnotationDefinitionDao.findByCohortId(
                        cohortAnnotationDefinition.getCohortId());

        assertEquals(expectedDBList.get(0), cohortAnnotationDefinition);
    }

    private CohortAnnotationDefinition createCohortAnnotationDefinition() {
        return new CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("annotation name")
                .annotationType(AnnotationType.BOOLEAN);
    }

}
