package org.pmiops.workbench.cohortreview.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ParticipantCohortAnnotationMapper {

  @Mapping(
      target = "annotationValueEnum",
      source = "dbParticipantCohortAnnotation.cohortAnnotationEnumValue.name")
  @Mapping(
      target = "annotationValueDate",
      source = "annotationValueDate",
      qualifiedByName = "dateToString")
  ParticipantCohortAnnotation dbModelToClient(
      DbParticipantCohortAnnotation dbParticipantCohortAnnotation);

  @Mapping(target = "cohortAnnotationEnumValue", ignore = true)
  @Mapping(target = "annotationValueDateString", source = "annotationValueDate")
  @Mapping(target = "annotationValueDate", ignore = true)
  DbParticipantCohortAnnotation clientToDbModel(
      ParticipantCohortAnnotation participantCohortAnnotation);
}
