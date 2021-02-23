package org.pmiops.workbench.cdr;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface CdrVersionMapper {

  @Mapping(source = "microarrayBigqueryDataset", target = "hasMicroarrayData")
  @Mapping(source = "archivalStatusEnum", target = "archivalStatus")
  @Mapping(source = "accessTier.shortName", target = "accessTierShortName")
  CdrVersion dbModelToClient(DbCdrVersion db);

  default boolean isNonEmpty(String s) {
    return s != null && !s.isEmpty();
  }
}
