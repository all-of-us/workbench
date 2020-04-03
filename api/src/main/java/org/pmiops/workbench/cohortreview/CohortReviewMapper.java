package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CohortReviewMapper {
  @Mapping(target = "etag", source = "version")
  @Mapping(target = "queryResultSize", ignore = true) // used for pagination. Design is in progress that will remove from this object, and currently mostly unused
  @Mapping(target = "page", ignore = true) // used for pagination. Design is in progress that will remove from this object, and currently mostly unused
  @Mapping(target = "pageSize", ignore = true) // used for pagination. Design is in progress that will remove from this object, and currently mostly unused
  @Mapping(target = "sortOrder", ignore = true) // used for pagination. Design is in progress that will remove from this object, and currently mostly unused
  @Mapping(target = "sortColumn", ignore = true) // used for pagination. Design is in progress that will remove from this object, and currently mostly unused
  @Mapping(target = "participantCohortStatuses", ignore = true) // this fetches all participants, and can be large, we don't want to fetch by default. May be removed from object pending design
  CohortReview dbModelToClient(DbCohortReview dbCohortReview);

  default ReviewStatus reviewStatusFromStorage(Short reviewStatus) {
    return DbStorageEnums.reviewStatusFromStorage(reviewStatus);
  }
}
