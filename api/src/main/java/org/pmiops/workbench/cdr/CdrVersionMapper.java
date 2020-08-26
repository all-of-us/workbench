package org.pmiops.workbench.cdr;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface CdrVersionMapper {

  @Mapping(source = "dataAccessLevelEnum", target = "dataAccessLevel")
  @Mapping(source = "archivalStatusEnum", target = "archivalStatus")
  CdrVersion dbModelToClient(DbCdrVersion db);
}
