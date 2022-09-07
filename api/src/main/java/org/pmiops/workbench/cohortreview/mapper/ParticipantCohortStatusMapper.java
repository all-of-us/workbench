package org.pmiops.workbench.cohortreview.mapper;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import java.sql.Date;
import java.time.Instant;
import java.util.stream.StreamSupport;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ParticipantCohortStatusMapper {

  @Mapping(
      target = "participantId",
      source = "dbParticipantCohortStatus.participantKey.participantId")
  @Mapping(target = "gender", ignore = true)
  @Mapping(target = "race", ignore = true)
  @Mapping(target = "ethnicity", ignore = true)
  @Mapping(target = "sexAtBirth", ignore = true)
  @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "dateToString")
  ParticipantCohortStatus dbModelToClient(
      DbParticipantCohortStatus dbParticipantCohortStatus,
      @Context Table<Long, CriteriaType, String> demoTable);

  @AfterMapping
  default void populateAfterMapping(
      @MappingTarget ParticipantCohortStatus participantCohortStatus,
      @Context Table<Long, CriteriaType, String> demoTable) {
    participantCohortStatus.setGender(
        demoTable.get(participantCohortStatus.getGenderConceptId(), CriteriaType.GENDER));
    participantCohortStatus.setRace(
        demoTable.get(participantCohortStatus.getRaceConceptId(), CriteriaType.RACE));
    participantCohortStatus.setEthnicity(
        demoTable.get(participantCohortStatus.getEthnicityConceptId(), CriteriaType.ETHNICITY));
    participantCohortStatus.setSexAtBirth(
        demoTable.get(participantCohortStatus.getSexAtBirthConceptId(), CriteriaType.SEX));
  }

  default DbParticipantCohortStatus fieldValueListToDbParticipantCohortStatus(
      FieldValueList row, Long cohortReviewId) {
    DbParticipantCohortStatus dbParticipantCohortStatus =
        new DbParticipantCohortStatus()
            .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
            .birthDate(getBirthDate(row));
    FieldValues.getLong(row, "person_id")
        .ifPresent(
            pId ->
                dbParticipantCohortStatus.setParticipantKey(
                    new DbParticipantCohortStatusKey(cohortReviewId, pId)));
    FieldValues.getLong(row, "gender_concept_id")
        .ifPresent(dbParticipantCohortStatus::setGenderConceptId);
    FieldValues.getLong(row, "race_concept_id")
        .ifPresent(dbParticipantCohortStatus::setRaceConceptId);
    FieldValues.getLong(row, "ethnicity_concept_id")
        .ifPresent(dbParticipantCohortStatus::setEthnicityConceptId);
    FieldValues.getLong(row, "sex_at_birth_concept_id")
        .ifPresent(dbParticipantCohortStatus::setSexAtBirthConceptId);
    FieldValues.getBoolean(row, "deceased").ifPresent(dbParticipantCohortStatus::setDeceased);
    return dbParticipantCohortStatus;
  }

  default ImmutableList<DbParticipantCohortStatus> tableResultToDbParticipantCohortStatus(
      TableResult tableResult, Long cohortReviewId) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(row -> fieldValueListToDbParticipantCohortStatus(row, cohortReviewId))
        .collect(ImmutableList.toImmutableList());
  }

  default Date getBirthDate(FieldValueList row) {
    String birthDateTimeString = FieldValues.getString(row, "birth_datetime").orElse(null);
    if (birthDateTimeString == null) {
      throw new BigQueryException(500, "birth_datetime is null at position: birth_datetime");
    }
    return new Date(
        Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000))
            .getTime());
  }
}
