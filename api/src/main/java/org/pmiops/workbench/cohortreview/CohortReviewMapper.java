package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.PageRequest;
import org.pmiops.workbench.utils.EnumConverter;

@Mapper(componentModel = "spring", uses = {EnumConverter.class})
public interface CohortReviewMapper {
  @Mapping(target = "participantId", source = "db.participantKey.participantId")
  @Mapping(target = "status", source = "db.statusEnum")
  @Mapping(target = "birthDate", source = "db.birthDate", dateFormat = "yyyy-MM-dd")
  org.pmiops.workbench.model.ParticipantCohortStatus toApiParticipant(ParticipantCohortStatus db);


  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "reviewStatus", source = "cohortReview.reviewStatusEnum")
  @Mapping(target = "creationTime", source = "cohortReview.creationTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
  CohortReview toApiCohortReviewWithPaging(org.pmiops.workbench.db.model.CohortReview cohortReview, PageRequest pageRequest);
}