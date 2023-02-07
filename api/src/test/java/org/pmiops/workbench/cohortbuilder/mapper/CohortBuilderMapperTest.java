package org.pmiops.workbench.cohortbuilder.mapper;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.*;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class CohortBuilderMapperTest {

  @Autowired private CohortBuilderMapper cohortBuilderMapper;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, CommonMappers.class, CohortBuilderMapperImpl.class})
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
            .parentCount(100L)
            .childCount(0L)
            .conceptId(12345L)
            .domainId(Domain.CONDITION.toString())
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
                    .addCount(200L)
                    .addParentCount(100L)
                    .addChildCount(null)
                    .addConceptId("12345")
                    .addDomainId(Domain.CONDITION.toString())
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
    assertThat(cohortBuilderMapper.dbModelToClient(new DbAgeTypeCountImpl("Age", 22, 2344L)))
        .isEqualTo(expectedAgeTypeCount);
  }

  @Test
  public void dbModelToClientSurveyVersion() {
    SurveyVersion expectedSurveyVersion =
        new SurveyVersion().surveyVersionConceptId(100L).displayName("May 2020").itemCount(2344L);
    assertThat(
            cohortBuilderMapper.dbModelToClient(new DbSurveyVersionImpl(100L, "May 2020", 2344L)))
        .isEqualTo(expectedSurveyVersion);
  }

  @ParameterizedTest(name = "dbModelToClientCardCount -> {0}")
  @MethodSource("domainCountParameters")
  public void dbModelToClientCardCount(String domain) {
    CardCount expected = new CardCount().domain(Domain.valueOf(domain)).name(domain).count(10L);
    assertThat(
            cohortBuilderMapper.dbModelToClient(
                new DbCardCount() {
                  public String getDomainId() {
                    return domain;
                  }

                  public String getName() {
                    return domain;
                  }

                  public Long getCount() {
                    return 10L;
                  }
                }))
        .isEqualTo(expected);
  }

  @Test
  public void tableResultToCohortChartData() {
    Field name = Field.of("name", LegacySQLTypeName.STRING);
    Field conceptId = Field.of("conceptId", LegacySQLTypeName.INTEGER);
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Schema schema = Schema.of(name, conceptId, count);

    FieldValue nameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "name");
    FieldValue conceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "77");
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "10");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(Arrays.asList(nameValue, conceptIdValue, countValue)));

    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    CohortChartData cohortChartData = new CohortChartData().name("name").conceptId(77L).count(10L);
    assertThat(cohortBuilderMapper.tableResultToCohortChartData(result))
        .isEqualTo(ImmutableList.of(cohortChartData));
  }

  @Test
  public void tableResultToDemoChartInfo() {
    Field name = Field.of("name", LegacySQLTypeName.STRING);
    Field race = Field.of("race", LegacySQLTypeName.STRING);
    Field ageRange = Field.of("ageRange", LegacySQLTypeName.STRING);
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Schema s = Schema.of(name, race, ageRange, count);

    FieldValue nameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "name");
    FieldValue raceValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "race");
    FieldValue ageRangeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2-11");
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "10");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(Arrays.asList(nameValue, raceValue, ageRangeValue, countValue)));

    TableResult result =
        new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    DemoChartInfo demoChartInfo = new DemoChartInfo().name("name").ageRange("2-11").count(10L);
    assertThat(cohortBuilderMapper.tableResultToDemoChartInfo(result))
        .isEqualTo(ImmutableList.of(demoChartInfo));
  }

  @Test
  public void tableResultToEthnicityInfo() {
    Field ethnicity = Field.of("ethnicity", LegacySQLTypeName.STRING);
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Schema s = Schema.of(ethnicity, count);

    FieldValue idValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "eth");
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "10");
    List<FieldValueList> tableRows =
        Collections.singletonList(FieldValueList.of(Arrays.asList(idValue, countValue)));

    TableResult result =
        new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    EthnicityInfo ethnicityInfo = new EthnicityInfo().ethnicity("eth").count(10L);
    assertThat(cohortBuilderMapper.tableResultToEthnicityInfo(result))
        .isEqualTo(ImmutableList.of(ethnicityInfo));
  }

  private static Stream<Arguments> domainCountParameters() {
    return Stream.of(
        Arguments.of("CONDITION", 10),
        Arguments.of("DRUG", 10),
        Arguments.of("MEASUREMENT", 10),
        Arguments.of("OBSERVATION", 10),
        Arguments.of("PROCEDURE", 10),
        Arguments.of("PHYSICAL_MEASUREMENT_CSS", 10),
        Arguments.of("SURVEY", 10));
  }

  static class DbAgeTypeCountImpl implements DbAgeTypeCount {
    private final String ageType;
    private final int age;
    private final long count;

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

  static class DbSurveyVersionImpl implements DbSurveyVersion {
    private final long surveyVersionConceptId;
    private final String displayName;
    private final long itemCount;

    public DbSurveyVersionImpl(long surveyVersionConceptId, String displayName, long itemCount) {
      this.surveyVersionConceptId = surveyVersionConceptId;
      this.displayName = displayName;
      this.itemCount = itemCount;
    }

    @Override
    public long getSurveyVersionConceptId() {
      return surveyVersionConceptId;
    }

    @Override
    public String getDisplayName() {
      return displayName;
    }

    @Override
    public long getItemCount() {
      return itemCount;
    }
  }
}
