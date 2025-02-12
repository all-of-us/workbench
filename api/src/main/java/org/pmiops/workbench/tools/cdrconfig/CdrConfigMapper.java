package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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

  @Mapping(source = "archivalStatus", target = "archivalStatusEnum")
  @Mapping(source = "accessTier", target = "accessTier", qualifiedByName = "toDbTierByShortName")
  DbCdrVersion toDbVersion(CdrVersionVO localVersion, @Context AccessTierDao accessTierDao);

  @Named("toDbTierByShortName")
  default DbAccessTier toDbTierByShortName(
      String accessTierShortName, @Context AccessTierDao accessTierDao) {
    return accessTierDao.findOneByShortName(accessTierShortName).orElse(null);
  }

  List<DbCdrVersion> toDbVersions(
      List<CdrVersionVO> localVersions, @Context AccessTierDao accessTierDao);

  default List<DbCdrVersion> cdrVersions(CdrConfigRecord cdrConfig, AccessTierDao accessTierDao) {
    return toDbVersions(cdrConfig.cdrVersions(), accessTierDao);
  }
}
