package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.utils.BigQueryUtils;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class ParticipantCohortStatusMapperTest {

  @Autowired private ParticipantCohortStatusMapper participantCohortStatusMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    ParticipantCohortStatusMapperImpl.class,
    CommonMappers.class
  })
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    Date birthDate = new Date(System.currentTimeMillis());

    ParticipantCohortStatus expectedParticipantCohortStatus =
        new ParticipantCohortStatus()
            .status(CohortStatus.INCLUDED)
            .participantId(1L)
            .birthDate(birthDate.toString())
            .deceased(false)
            .ethnicityConceptId(1L)
            .ethnicity("Latino")
            .raceConceptId(2L)
            .race("White")
            .sexAtBirthConceptId(3L)
            .sexAtBirth("Male")
            .genderConceptId(4L)
            .gender("Man");
    Table<Long, CriteriaType, String> demoTable = HashBasedTable.create();
    demoTable.put(1L, CriteriaType.ETHNICITY, "Latino");
    demoTable.put(2L, CriteriaType.RACE, "White");
    demoTable.put(3L, CriteriaType.SEX, "Male");
    demoTable.put(4L, CriteriaType.GENDER, "Man");

    assertThat(
            participantCohortStatusMapper.dbModelToClient(
                new DbParticipantCohortStatus()
                    .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.INCLUDED))
                    .participantKey(
                        new DbParticipantCohortStatusKey().participantId(1L).cohortReviewId(1L))
                    .birthDate(birthDate)
                    .deceased(false)
                    .ethnicityConceptId(1L)
                    .raceConceptId(2L)
                    .sexAtBirthConceptId(3L)
                    .genderConceptId(4L),
                demoTable))
        .isEqualTo(expectedParticipantCohortStatus);
  }

  @Test
  public void tableResultToDbParticipantCohortStatus() {
    Field personId = Field.of("person_id", LegacySQLTypeName.INTEGER);
    Field birthDatetime = Field.of("birth_datetime", LegacySQLTypeName.DATETIME);
    Field genderConceptId = Field.of("gender_concept_id", LegacySQLTypeName.INTEGER);
    Field raceConceptId = Field.of("race_concept_id", LegacySQLTypeName.INTEGER);
    Field ethnicityConceptId = Field.of("ethnicity_concept_id", LegacySQLTypeName.INTEGER);
    Field sexAtBirthConceptId = Field.of("sex_at_birth_concept_id", LegacySQLTypeName.INTEGER);
    Field deceased = Field.of("deceased", LegacySQLTypeName.BOOLEAN);
    Schema s =
        Schema.of(
            personId,
            birthDatetime,
            genderConceptId,
            raceConceptId,
            ethnicityConceptId,
            sexAtBirthConceptId,
            deceased);

    FieldValue personIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue birthDatetimeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "-565401600.0");
    FieldValue genderConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue raceConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue ethnicityConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue sexAtBirthConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "4");
    FieldValue deceasedValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "false");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    personIdValue,
                    birthDatetimeValue,
                    genderConceptIdValue,
                    raceConceptIdValue,
                    ethnicityConceptIdValue,
                    sexAtBirthConceptIdValue,
                    deceasedValue)));

    TableResult result =
        BigQueryUtils.newTableResult(
            s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    Date birthDate =
        participantCohortStatusMapper.getBirthDate(result.iterateAll().iterator().next());
    DbParticipantCohortStatus dbParticipantCohortStatus =
        new DbParticipantCohortStatus()
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
            .participantKey(new DbParticipantCohortStatusKey().participantId(1L).cohortReviewId(1L))
            .birthDate(birthDate)
            .deceased(false)
            .ethnicityConceptId(3L)
            .raceConceptId(2L)
            .sexAtBirthConceptId(4L)
            .genderConceptId(1L);
    assertThat(participantCohortStatusMapper.tableResultToDbParticipantCohortStatus(result, 1L))
        .isEqualTo(ImmutableList.of(dbParticipantCohortStatus));
  }
}
