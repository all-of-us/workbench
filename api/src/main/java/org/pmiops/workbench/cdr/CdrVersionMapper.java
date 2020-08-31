package org.pmiops.workbench.cdr;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.utils.mappers.MapStructConfig;
import org.pmiops.workbench.utils.mappers.TimeMappers;

@Mapper(
    config = MapStructConfig.class,
    uses = {TimeMappers.class})
public interface CdrVersionMapper {

  @Mapping(source = "microarrayBigqueryDataset", target = "hasMicroarrayData")
  @Mapping(source = "dataAccessLevelEnum", target = "dataAccessLevel")
  @Mapping(source = "archivalStatusEnum", target = "archivalStatus")
  CdrVersion dbModelToClient(DbCdrVersion db);

  default boolean isNonEmpty(String s) {
    return s != null && !s.isEmpty();
  }
}
