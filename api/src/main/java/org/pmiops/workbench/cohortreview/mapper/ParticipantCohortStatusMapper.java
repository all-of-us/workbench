package org.pmiops.workbench.cohortreview.mapper;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.ParticipantCohortStatus;
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
      @Context MultiKeyMap<Object, String> demographicsMap);

  @AfterMapping
  default void populateAfterMapping(
      @MappingTarget ParticipantCohortStatus participantCohortStatus,
      @Context MultiKeyMap<Object, String> demographicsMap) {
    participantCohortStatus.setGender(
        demographicsMap.get(participantCohortStatus.getGenderConceptId(), CriteriaType.GENDER));
    participantCohortStatus.setRace(
        demographicsMap.get(participantCohortStatus.getRaceConceptId(), CriteriaType.RACE));
    participantCohortStatus.setEthnicity(
        demographicsMap.get(
            participantCohortStatus.getEthnicityConceptId(), CriteriaType.ETHNICITY));
    participantCohortStatus.setSexAtBirth(
        demographicsMap.get(participantCohortStatus.getSexAtBirthConceptId(), CriteriaType.SEX));
  }
}
