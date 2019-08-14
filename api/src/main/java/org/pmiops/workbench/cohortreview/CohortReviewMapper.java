package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.PageRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.utils.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CohortReviewMapper {

  @Mapping(target = "participantId", source = "db.participantKey.participantId")
  @Mapping(target = "status", source = "db.statusEnum")
  @Mapping(target = "birthDate", source = "db.birthDate", dateFormat = "yyyy-MM-dd")
  org.pmiops.workbench.model.ParticipantCohortStatus toApiParticipant(ParticipantCohortStatus db);

  @Mapping(target = "reviewStatus", source = "cohortReview.reviewStatusEnum")
  @Mapping(target = "creationTime", source = "creationTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Mapping(target = "etag", source = "version", qualifiedByName = "etag")
  CohortReview toApiCohortReview(org.pmiops.workbench.db.model.CohortReview cohortReview);

  @Mapping(target = "reviewStatus", source = "cohortReview.reviewStatusEnum")
  @Mapping(
      target = "creationTime",
      source = "cohortReview.creationTime",
      dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Mapping(target = "lastModifiedTime", ignore = true)
  CohortReview toApiCohortReviewWithPaging(
      org.pmiops.workbench.db.model.CohortReview cohortReview, PageRequest pageRequest);

  @Mapping(target = "annotationValueEnum", source = "cohortAnnotationEnumValue")
  @Mapping(
      target = "annotationValueDate",
      source = "annotationValueDate",
      dateFormat = "yyyy-MM-dd")
  ParticipantCohortAnnotation toApi(
      org.pmiops.workbench.db.model.ParticipantCohortAnnotation annotation);

  @Mapping(target = "annotationValueDate", ignore = true)
  @Mapping(target = "annotationValueDateString", source = "annotationValueDate")
  org.pmiops.workbench.db.model.ParticipantCohortAnnotation toEntity(
      ParticipantCohortAnnotation annotation);

  default String cohortAnnotationEnumValueToString(CohortAnnotationEnumValue enumValue) {
    return enumValue == null ? null : enumValue.getName();
  }
}
