package org.pmiops.workbench.access;

import java.sql.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, AccessModuleNameMapper.class})
public interface UserAccessModuleMapper {
  @Mapping(target = "moduleName", source = "source.accessModule.name")
  @Mapping(target = "completionEpochMillis", source = "source.completionTime")
  @Mapping(target = "bypassEpochMillis", source = "source.bypassTime")
  @Mapping(target = "expirationEpochMillis", source = "expirationTime")
  AccessModuleStatus dbToModule(DbUserAccessModule source, Timestamp expirationTime);
}
