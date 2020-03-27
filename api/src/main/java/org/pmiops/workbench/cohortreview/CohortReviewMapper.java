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
  @Mapping(target="etag", source="version")
  CohortReview dbModelToClient(DbCohortReview dbCohortReview);

  default ReviewStatus reviewStatusFromStorage(Short reviewStatus) {
    return DbStorageEnums.reviewStatusFromStorage(reviewStatus);
  }
}
