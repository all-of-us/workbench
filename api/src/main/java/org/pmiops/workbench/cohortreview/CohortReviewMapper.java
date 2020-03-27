package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CohortReviewMapper {
  CohortReview dbModelToClient(DbCohortReview dbCohortReview);
}
