package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface CdrConfigMapper {

  DbAccessTier toDbTier(AccessTierConfig localTier);

  List<DbAccessTier> toDbTiers(List<AccessTierConfig> localTiers);

  default List<DbAccessTier> accessTiers(CdrConfigRecord cdrConfig) {
    return toDbTiers(cdrConfig.accessTiers());
  }

  // MapStruct gets the accessTier mapping wrong, by choosing the wrong AccessTierDao method
  // (TODO: why?) but we can specify it explicitly using the AfterMapping below
  @Mapping(target = "accessTier", ignore = true)
  @Mapping(source = "archivalStatus", target = "archivalStatusEnum")
  DbCdrVersion toDbVersion(CdrVersionVO localVersion, @Context AccessTierDao accessTierDao);

  @AfterMapping
  default void populateAccessTier(
      CdrVersionVO localVersion,
      @MappingTarget DbCdrVersion dbCdrVersion,
      @Context AccessTierDao accessTierDao) {
    accessTierDao
        .findOneByShortName(localVersion.accessTier)
        .ifPresent(dbCdrVersion::setAccessTier);
  }

  List<DbCdrVersion> toDbVersions(
      List<CdrVersionVO> localVersions, @Context AccessTierDao accessTierDao);

  default List<DbCdrVersion> cdrVersions(CdrConfigRecord cdrConfig, AccessTierDao accessTierDao) {
    return toDbVersions(cdrConfig.cdrVersions(), accessTierDao);
  }
}
