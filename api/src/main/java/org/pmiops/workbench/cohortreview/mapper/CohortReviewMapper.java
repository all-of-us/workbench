package org.pmiops.workbench.cohortreview.mapper;

import static org.pmiops.workbench.model.FilterColumns.*;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface CohortReviewMapper {
  @Mapping(target = "etag", source = "version", qualifiedByName = "versionToEtag")
  // this fetches all participants, and can be large, we don't want to fetch by
  // default. May be removed from object pending design
  @Mapping(target = "participantCohortStatuses", ignore = true)
  CohortReview dbModelToClient(DbCohortReview dbCohortReview);

  @Mapping(target = "version", source = "cohortReview.etag", qualifiedByName = "etagToCdrVersion")
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "reviewStatusEnum", ignore = true)
  DbCohortReview clientToDbModel(CohortReview cohortReview);

  @Mapping(target = "version", source = "cohortReview.etag", qualifiedByName = "etagToCdrVersion")
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "reviewStatusEnum", ignore = true)
  DbCohortReview clientToDbModel(CohortReview cohortReview, @Context DbUser creator);

  @AfterMapping
  default void populateAfterMapping(
      @MappingTarget DbCohortReview dbCohortReview, @Context DbUser creator) {
    dbCohortReview.creator(creator);
  }

  default ParticipantChartData fieldValueListToParticipantChartData(FieldValueList row) {
    ParticipantChartData participantChartData = new ParticipantChartData();
    FieldValues.getString(row, "standardName").ifPresent(participantChartData::setStandardName);
    FieldValues.getString(row, "standardVocabulary")
        .ifPresent(participantChartData::setStandardVocabulary);
    FieldValues.getString(row, "startDate").ifPresent(participantChartData::setStartDate);
    FieldValues.getLong(row, "ageAtEvent")
        .ifPresent(age -> participantChartData.setAgeAtEvent(age.intValue()));
    FieldValues.getLong(row, "rank")
        .ifPresent(rank -> participantChartData.setRank(rank.intValue()));
    return participantChartData;
  }

  default ImmutableList<ParticipantChartData> tableResultToParticipantChartData(
      TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToParticipantChartData)
        .collect(ImmutableList.toImmutableList());
  }

  default ParticipantData fieldValueListToParticipantData(FieldValueList row, Domain domain) {
    DateTimeFormatter df =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz").withZone(ZoneId.of("UTC"));

    ParticipantData participantData = new ParticipantData();
    if (!domain.equals(Domain.SURVEY)) {
      if (!row.get(START_DATETIME.toString()).isNull()) {
        participantData.setItemDate(
            df.format(FieldValues.getInstant(row.get(START_DATETIME.toString()))));
      }
      FieldValues.getString(row, DOMAIN.toString()).ifPresent(participantData::setDomain);
      FieldValues.getString(row, STANDARD_NAME.toString())
          .ifPresent(participantData::setStandardName);
      FieldValues.getLong(row, AGE_AT_EVENT.toString())
          .ifPresent(age -> participantData.setAgeAtEvent(age.intValue()));
      FieldValues.getLong(row, STANDARD_CONCEPT_ID.toString())
          .ifPresent(participantData::setStandardConceptId);
      FieldValues.getLong(row, SOURCE_CONCEPT_ID.toString())
          .ifPresent(participantData::setSourceConceptId);
      FieldValues.getString(row, STANDARD_VOCABULARY.toString())
          .ifPresent(participantData::setStandardVocabulary);
      FieldValues.getString(row, SOURCE_VOCABULARY.toString())
          .ifPresent(participantData::setSourceVocabulary);
      FieldValues.getString(row, SOURCE_NAME.toString()).ifPresent(participantData::setSourceName);
      FieldValues.getString(row, SOURCE_CODE.toString()).ifPresent(participantData::setSourceCode);
      FieldValues.getString(row, STANDARD_CODE.toString())
          .ifPresent(participantData::setStandardCode);
      FieldValues.getString(row, VALUE_AS_NUMBER.toString()).ifPresent(participantData::setValue);
      FieldValues.getString(row, VISIT_TYPE.toString()).ifPresent(participantData::setVisitType);
      FieldValues.getString(row, NUM_MENTIONS.toString())
          .ifPresent(participantData::setNumMentions);
      if (!row.get(FIRST_MENTION.toString()).isNull()) {
        participantData.setFirstMention(
            df.format(FieldValues.getInstant(row.get(FIRST_MENTION.toString()))));
      }
      if (!row.get(LAST_MENTION.toString()).isNull()) {
        participantData.setLastMention(
            df.format(FieldValues.getInstant(row.get(LAST_MENTION.toString()))));
      }
      FieldValues.getString(row, UNIT.toString()).ifPresent(participantData::setUnit);
      FieldValues.getString(row, DOSE.toString()).ifPresent(participantData::setDose);
      FieldValues.getString(row, STRENGTH.toString()).ifPresent(participantData::setStrength);
      FieldValues.getString(row, ROUTE.toString()).ifPresent(participantData::setRoute);
      FieldValues.getString(row, REF_RANGE.toString()).ifPresent(participantData::setRefRange);
    } else {
      if (!row.get(START_DATETIME.toString()).isNull()) {
        participantData.setItemDate(
            df.format(FieldValues.getInstant(row.get(START_DATETIME.toString()))));
      }
      FieldValues.getString(row, SURVEY_NAME.toString()).ifPresent(participantData::setSurvey);
      FieldValues.getString(row, QUESTION.toString()).ifPresent(participantData::setQuestion);
      FieldValues.getString(row, ANSWER.toString()).ifPresent(participantData::setAnswer);
    }
    return participantData;
  }

  default ImmutableList<ParticipantData> tableResultToVocabulary(
      TableResult tableResult, Domain domain) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(row -> fieldValueListToParticipantData(row, domain))
        .collect(ImmutableList.toImmutableList());
  }

  default Vocabulary fieldValueListToVocabulary(FieldValueList row) {
    Vocabulary vocabulary = new Vocabulary();
    FieldValues.getString(row, "domain").ifPresent(vocabulary::setDomain);
    FieldValues.getString(row, "type").ifPresent(vocabulary::setType);
    FieldValues.getString(row, "vocabulary").ifPresent(vocabulary::setVocabulary);
    return vocabulary;
  }

  default ImmutableList<Vocabulary> tableResultToVocabulary(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToVocabulary)
        .collect(ImmutableList.toImmutableList());
  }
}
