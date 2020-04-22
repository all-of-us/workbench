package org.pmiops.workbench.utils;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.pmiops.workbench.config.BigQueryConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT,
    uses = {
        CommonMappers.class,
        DbStorageEnums.class
    })
public interface CdrVersionMapper {

  @Mapping(source = "cdrVersion.dataAccessLevel", target = "dataAccessLevelEnum")
//  @Mapping(source = "isDefault", target = "isDefault")
//  @Mapping(source = "dataAccessLevel", target = "dataAccessLevelEnum")
  @Mapping(source = "cdrVersion.archivalStatus", target = "archivalStatusEnum")
  @Mapping(source = "foo", target = "releaseNumber")
  @Mapping(source = "bigQueryConfig.projectId", target = "bigqueryProject")
  @Mapping(source = "bigQueryConfig.dataSetId", target = "bigqueryDataset")
  @Mapping(source = "foo", target = "numParticipants")
  @Mapping(source = "foo", target = "cdrDbName")
  @Mapping(source = "foo", target = "elasticIndexBaseName")
  DbCdrVersion toDbCdrVersion(CdrVersion cdrVersion,
      boolean isDefault,
      BigQueryConfig bigQueryConfig);

  default String cdrVersionIdToString(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }
}
