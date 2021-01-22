package org.pmiops.workbench.cohortreview.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface CohortReviewMapper {
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
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
}
