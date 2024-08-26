package org.pmiops.workbench.tools.cdrconfig;

import jakarta.annotation.Nullable;
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
public interface CdrConfigVOMapper {

  DbAccessTier toDbTier(AccessTierVO localTier);

  List<DbAccessTier> toDbTiers(List<AccessTierVO> localTiers);

  default List<DbAccessTier> accessTiers(CdrConfigVO cdrConfig) {
    return toDbTiers(cdrConfig.accessTiers);
  }

  @Mapping(target = "accessTier", source = "accessTier", qualifiedByName = "useDao")
  @Mapping(source = "archivalStatus", target = "archivalStatusEnum")
  DbCdrVersion toDbVersion(CdrVersionVO localVersion, @Context AccessTierDao accessTierDao);

  @Named("useDao")
  @Nullable
  default DbAccessTier useDao(String accessTierShortName, @Context AccessTierDao accessTierDao) {
    return accessTierDao.findOneByShortName(accessTierShortName).orElse(null);
  }

  List<DbCdrVersion> toDbVersions(
      List<CdrVersionVO> localVersions, @Context AccessTierDao accessTierDao);

  default List<DbCdrVersion> cdrVersions(CdrConfigVO cdrConfig, AccessTierDao accessTierDao) {
    return toDbVersions(cdrConfig.cdrVersions, accessTierDao);
  }
}
