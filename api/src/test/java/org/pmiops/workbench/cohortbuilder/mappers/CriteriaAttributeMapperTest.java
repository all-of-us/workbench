package org.pmiops.workbench.cohortbuilder.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CriteriaAttributeMapperTest {

  @Autowired private CriteriaAttributeMapper criteriaAttributeMapper;

  private DbCriteriaAttribute dbCriteriaAttribute;

  @TestConfiguration
  @Import({CriteriaAttributeMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    dbCriteriaAttribute =
        DbCriteriaAttribute.builder()
            .addId(1)
            .addConceptId(1)
            .addValueAsConceptId(1)
            .addConceptName("name")
            .addType("type")
            .addEstCount("2")
            .build();
  }

  @Test
  public void dbModelToClient() {
    CriteriaAttribute expectedCriteriaAttribute =
        new CriteriaAttribute()
            .id(1L)
            .valueAsConceptId(1L)
            .conceptName("name")
            .type("type")
            .estCount("2");

    assertThat(criteriaAttributeMapper.dbModelToClient(dbCriteriaAttribute))
        .isEqualTo(expectedCriteriaAttribute);
  }
}
