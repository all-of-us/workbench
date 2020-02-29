package org.pmiops.workbench.cohortbuilder.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CriteriaMapperTest {

  @Autowired private CriteriaMapper criteriaMapper;

  private DbCriteria dbCriteria;

  @TestConfiguration
  @Import({CriteriaMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    dbCriteria =
        DbCriteria.builder()
            .addId(1)
            .addParentId(1)
            .addType(CriteriaType.ICD9CM.toString())
            .addSubtype(CriteriaSubType.LAB.toString())
            .addCode("code")
            .addName("name")
            .addGroup(true)
            .addSelectable(true)
            .addCount("200")
            .addConceptId("12345")
            .addDomainId(DomainType.CONDITION.toString())
            .addAttribute(true)
            .addPath("path")
            .addSynonyms("syn")
            .addValue("value")
            .addHierarchy(true)
            .addAncestorData(true)
            .addStandard(true)
            .build();
  }

  @Test
  public void dbModelToClient() {
    Criteria expectedCriteria =
        new Criteria()
            .id(1L)
            .parentId(1L)
            .type(CriteriaType.ICD9CM.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .code("code")
            .name("name")
            .group(true)
            .selectable(true)
            .count(200L)
            .conceptId(12345L)
            .domainId(DomainType.CONDITION.toString())
            .hasAttributes(true)
            .path("path")
            .value("value")
            .hasHierarchy(true)
            .hasAncestorData(true)
            .isStandard(true);

    assertThat(criteriaMapper.dbModelToClient(dbCriteria)).isEqualTo(expectedCriteria);
  }
}
