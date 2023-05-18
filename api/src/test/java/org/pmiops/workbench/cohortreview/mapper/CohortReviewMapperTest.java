package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.*;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class CohortReviewMapperTest {

  @Autowired private CohortReviewMapper cohortReviewMapper;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, CohortReviewMapperImpl.class, CommonMappers.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    CohortReview expectedCohortReview =
        new CohortReview()
            .cohortReviewId(1L)
            .cdrVersionId(1L)
            .cohortDefinition("def")
            .cohortId(1L)
            .cohortName("name")
            .cohortReviewId(1L)
            .creationTime(timestamp.getTime())
            .description("desc")
            .etag(Etags.fromVersion(1))
            .lastModifiedTime(timestamp.getTime())
            .matchedParticipantCount(200L)
            .reviewSize(10L)
            .reviewStatus(ReviewStatus.CREATED)
            .reviewedCount(10L);
    assertThat(
            cohortReviewMapper.dbModelToClient(
                new DbCohortReview()
                    .cohortReviewId(1L)
                    .cohortDefinition("def")
                    .cohortId(1L)
                    .cohortName("name")
                    .version(1)
                    .reviewSize(10L)
                    .reviewStatus(Short.valueOf("1"))
                    .reviewedCount(10L)
                    .cdrVersionId(1L)
                    .creationTime(timestamp)
                    .lastModifiedTime(timestamp)
                    .description("desc")
                    .matchedParticipantCount(200)))
        .isEqualTo(expectedCohortReview);
  }

  @Test
  public void tableResultToParticipantChartData() {
    Field standardName = Field.of("standardName", LegacySQLTypeName.STRING);
    Field standardVocabulary = Field.of("standardVocabulary", LegacySQLTypeName.STRING);
    Field startDate = Field.of("startDate", LegacySQLTypeName.DATETIME);
    Field ageAtEvent = Field.of("ageAtEvent", LegacySQLTypeName.INTEGER);
    Field rank = Field.of("rank", LegacySQLTypeName.INTEGER);
    Schema s = Schema.of(standardName, standardVocabulary, startDate, ageAtEvent, rank);

    FieldValue standardNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardName");
    FieldValue standardVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardVocabulary");
    FieldValue startDateValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1989-04-19 14:29:00 UTC");
    FieldValue ageAtEventValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "22");
    FieldValue rankValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    standardNameValue,
                    standardVocabularyValue,
                    startDateValue,
                    ageAtEventValue,
                    rankValue)));

    TableResult result =
        new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    ParticipantChartData participantChartData =
        new ParticipantChartData()
            .standardName("standardName")
            .standardVocabulary("standardVocabulary")
            .startDate("1989-04-19 14:29:00 UTC")
            .ageAtEvent(22)
            .rank(1);
    assertThat(cohortReviewMapper.tableResultToParticipantChartData(result))
        .isEqualTo(ImmutableList.of(participantChartData));
  }

  @Test
  public void tableResultToParticipantData() {
    Field startDatetime = Field.of("START_DATETIME", LegacySQLTypeName.DATETIME);
    Field domain = Field.of("DOMAIN", LegacySQLTypeName.STRING);
    Field standardName = Field.of("STANDARD_NAME", LegacySQLTypeName.STRING);
    Field ageAtEvent = Field.of("AGE_AT_EVENT", LegacySQLTypeName.INTEGER);
    Field standardConceptId = Field.of("STANDARD_CONCEPT_ID", LegacySQLTypeName.INTEGER);
    Field sourceConceptId = Field.of("SOURCE_CONCEPT_ID", LegacySQLTypeName.INTEGER);
    Field standardVocabulary = Field.of("STANDARD_VOCABULARY", LegacySQLTypeName.STRING);
    Field sourceVocabulary = Field.of("SOURCE_VOCABULARY", LegacySQLTypeName.STRING);
    Field sourceName = Field.of("SOURCE_NAME", LegacySQLTypeName.STRING);
    Field sourceCode = Field.of("SOURCE_CODE", LegacySQLTypeName.STRING);
    Field standardCode = Field.of("STANDARD_CODE", LegacySQLTypeName.STRING);
    Field valueAsNumber = Field.of("VALUE_AS_NUMBER", LegacySQLTypeName.STRING);
    Field visitType = Field.of("VISIT_TYPE", LegacySQLTypeName.STRING);
    Field numMentions = Field.of("NUM_MENTIONS", LegacySQLTypeName.STRING);
    Field firstMention = Field.of("FIRST_MENTION", LegacySQLTypeName.STRING);
    Field lastMention = Field.of("LAST_MENTION", LegacySQLTypeName.STRING);
    Field unit = Field.of("UNIT", LegacySQLTypeName.STRING);
    Field dose = Field.of("DOSE", LegacySQLTypeName.STRING);
    Field strength = Field.of("STRENGTH", LegacySQLTypeName.STRING);
    Field route = Field.of("ROUTE", LegacySQLTypeName.STRING);
    Field refRange = Field.of("REF_RANGE", LegacySQLTypeName.STRING);
    Schema schema =
        Schema.of(
            startDatetime,
            domain,
            standardName,
            ageAtEvent,
            standardConceptId,
            sourceConceptId,
            standardVocabulary,
            sourceVocabulary,
            sourceName,
            sourceCode,
            standardCode,
            valueAsNumber,
            visitType,
            numMentions,
            firstMention,
            lastMention,
            unit,
            dose,
            strength,
            route,
            refRange);
    FieldValue startDatetimeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue domainValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "domain");
    FieldValue standardNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardName");
    FieldValue ageAtEventValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue standardConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue sourceConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue standardVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardVocabulary");
    FieldValue sourceVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceVocabulary");
    FieldValue sourceNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceName");
    FieldValue sourceCodeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceCode");
    FieldValue standardCodeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardCode");
    FieldValue valueAsNumberValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "valueAsNumber");
    FieldValue visitTypeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "visitType");
    FieldValue numMentionsValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue firstMentionValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue lastMentionValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue unitValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue doseValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue strengthValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue routeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "4");
    FieldValue refRangeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "5");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    startDatetimeValue,
                    domainValue,
                    standardNameValue,
                    ageAtEventValue,
                    standardConceptIdValue,
                    sourceConceptIdValue,
                    standardVocabularyValue,
                    sourceVocabularyValue,
                    sourceNameValue,
                    sourceCodeValue,
                    standardCodeValue,
                    valueAsNumberValue,
                    visitTypeValue,
                    numMentionsValue,
                    firstMentionValue,
                    lastMentionValue,
                    unitValue,
                    doseValue,
                    strengthValue,
                    routeValue,
                    refRangeValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    ParticipantData participantData =
        new ParticipantData()
            .itemDate("1970-01-01 00:00:01 UTC")
            .domain("domain")
            .standardName("standardName")
            .ageAtEvent(1)
            .standardConceptId(1L)
            .sourceConceptId(2L)
            .standardVocabulary("standardVocabulary")
            .sourceVocabulary("sourceVocabulary")
            .sourceName("sourceName")
            .sourceCode("sourceCode")
            .standardCode("standardCode")
            .value("valueAsNumber")
            .visitType("visitType")
            .numMentions("3")
            .firstMention("1970-01-01 00:00:03 UTC")
            .lastMention("1970-01-01 00:00:03 UTC")
            .unit("1")
            .dose("2")
            .strength("3")
            .refRange("5")
            .route("4");
    assertThat(cohortReviewMapper.tableResultToVocabulary(result, Domain.CONDITION))
        .isEqualTo(ImmutableList.of(participantData));
  }

  @Test
  public void tableResultToVocabulary() {
    Field domain = Field.of("domain", LegacySQLTypeName.STRING);
    Field type = Field.of("type", LegacySQLTypeName.STRING);
    Field vocabulary1 = Field.of("vocabulary", LegacySQLTypeName.STRING);
    Schema s = Schema.of(domain, type, vocabulary1);

    FieldValue domainValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "domain");
    FieldValue typeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "type");
    FieldValue vocabularyValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "vocabulary");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(Arrays.asList(domainValue, typeValue, vocabularyValue)));

    TableResult result =
        new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    Vocabulary vocabulary = new Vocabulary().domain("domain").type("type").vocabulary("vocabulary");
    assertThat(cohortReviewMapper.tableResultToVocabulary(result))
        .isEqualTo(ImmutableList.of(vocabulary));
  }
}
