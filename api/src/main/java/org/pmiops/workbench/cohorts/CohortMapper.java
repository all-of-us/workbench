package org.pmiops.workbench.cohorts;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, UserDao.class})
public interface CohortMapper {

  @Mapping(target = "cohortReviews", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "cohortId", source = "id")
  DbCohort clientToDbModel(Cohort source);

  @Mapping(target = "id", source = "cohortId")
  @Mapping(target = "etag", source = "version", qualifiedByName = "versionToEtag")
  Cohort dbModelToClient(DbCohort destination);
}
