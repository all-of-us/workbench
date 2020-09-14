package org.pmiops.workbench.cohortbuilder.mapper;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SurveyVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CohortBuilderMapperTest {

  @Autowired private CohortBuilderMapper cohortBuilderMapper;

  @TestConfiguration
  @Import({CohortBuilderMapperImpl.class})
  static class Configuration {}

  @Test
  public void dbModelToClientCriteria() {
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

    assertThat(
            cohortBuilderMapper.dbModelToClient(
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
                    .build()))
        .isEqualTo(expectedCriteria);
  }

  @Test
  public void dbModelToClientDataFilter() {
    DataFilter expectedDataFilter =
        new DataFilter().dataFilterId(1L).displayName("displayName").name("name");
    assertThat(
            cohortBuilderMapper.dbModelToClient(
                DbDataFilter.builder()
                    .addDataFilterId(1L)
                    .addDisplayName("displayName")
                    .addName("name")
                    .build()))
        .isEqualTo(expectedDataFilter);
  }

  @Test
  public void dbModelToClientCriteriaAttribute() {
    CriteriaAttribute expectedCriteriaAttribute =
        new CriteriaAttribute()
            .id(1L)
            .valueAsConceptId(1L)
            .conceptName("name")
            .type("type")
            .estCount("2");

    assertThat(
            cohortBuilderMapper.dbModelToClient(
                DbCriteriaAttribute.builder()
                    .addId(1)
                    .addConceptId(1)
                    .addValueAsConceptId(1)
                    .addConceptName("name")
                    .addType("type")
                    .addEstCount("2")
                    .build()))
        .isEqualTo(expectedCriteriaAttribute);
  }

  @Test
  public void dbModelToClientAgeTypeCount() {
    AgeTypeCount expectedAgeTypeCount = new AgeTypeCount().ageType("Age").age(22).count(2344L);
    assertThat(
            cohortBuilderMapper.dbModelToClient(
                new CohortBuilderMapperTest.DbAgeTypeCountImpl("Age", 22, 2344L)))
        .isEqualTo(expectedAgeTypeCount);
  }

  @Test
  public void dbModelToClientSurveyVersion() {
    SurveyVersion expectedSurveyVersion =
        new SurveyVersion().surveyId(100L).version("May 2020").itemCount(2344L);
    assertThat(
            cohortBuilderMapper.dbModelToClient(
                new CohortBuilderMapperTest.DbSurveyVersionImpl(100L, "May 2020", 2344L)))
        .isEqualTo(expectedSurveyVersion);
  }

  class DbAgeTypeCountImpl implements DbAgeTypeCount {
    private String ageType;
    private int age;
    private long count;

    public DbAgeTypeCountImpl(String ageType, int age, long count) {
      this.ageType = ageType;
      this.age = age;
      this.count = count;
    }

    @Override
    public String getAgeType() {
      return ageType;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public long getCount() {
      return count;
    }
  }

  class DbSurveyVersionImpl implements DbSurveyVersion {
    private long surveyId;
    private String version;
    private long itemCount;

    public DbSurveyVersionImpl(long surveyId, String version, long itemCount) {
      this.surveyId = surveyId;
      this.version = version;
      this.itemCount = itemCount;
    }

    @Override
    public long getSurveyId() {
      return surveyId;
    }

    @Override
    public String getVersion() {
      return version;
    }

    @Override
    public long getItemCount() {
      return itemCount;
    }
  }
}
