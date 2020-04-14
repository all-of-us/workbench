package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface CohortReviewMapper {
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  // used for pagination. Effectively deprecated, to remove with RW-4706
  @Mapping(target = "queryResultSize", ignore = true)
  @Mapping(target = "page", ignore = true)
  @Mapping(target = "pageSize", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  @Mapping(target = "sortColumn", ignore = true)
  // this fetches all participants, and can be large, we don't want to fetch by
  // default. May be removed from object pending design
  @Mapping(target = "participantCohortStatuses", ignore = true)
  CohortReview dbModelToClient(DbCohortReview dbCohortReview);
}
