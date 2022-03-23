package org.pmiops.workbench.access;

import java.sql.Timestamp;
import javax.annotation.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, AccessModuleNameMapper.class},
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface UserAccessModuleMapper {
  @Mapping(target = "moduleNameTemp", source = "accessModule.name")
  @Mapping(target = "moduleName", source = "accessModule.name")
  @Mapping(target = "completionEpochMillis", source = "completionTime")
  @Mapping(target = "bypassEpochMillis", source = "bypassTime")
  // expirationEpochMillis is derived value, will calculate & set it later.
  @Mapping(target = "expirationEpochMillis", ignore = true)
  AccessModuleStatus dbToModule(DbUserAccessModule source, @Context Timestamp expirationTime);

  @AfterMapping
  default void afterMappingDbToModel(
      @MappingTarget AccessModuleStatus accessModuleStatus,
      @Nullable @Context Timestamp expirationTime) {
    if (expirationTime != null) {
      accessModuleStatus.setExpirationEpochMillis(expirationTime.getTime());
    }
  }
}
