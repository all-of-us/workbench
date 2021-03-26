package org.pmiops.workbench.tools.cdrconfig;

import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
    uses = {CommonMappers.class, DbStorageEnums.class, AccessTierDao.class})
public interface CdrConfigVOMapper {

  default List<DbAccessTier> makeRandomCollection() {
    List<DbAccessTier> tiers = new ArrayList<>();
    tiers.add(new DbAccessTier());
    return tiers;
  }

  DbAccessTier toDbTier(AccessTierVO localTier);

  List<DbAccessTier> toDbTiers(List<AccessTierVO> localTiers);

  default List<DbAccessTier> accessTiers(CdrConfigVO cdrConfig) {
    return toDbTiers(cdrConfig.accessTiers);
  }

  // currently unambiguous, but adding the qualifiedByName to future-proof
  @Mapping(source = "accessTier", target = "accessTier", qualifiedByName = "findOneByShortName")
  @Mapping(source = "archivalStatus", target = "archivalStatusEnum")
  DbCdrVersion toDbVersion(CdrVersionVO localVersion);

  List<DbCdrVersion> toDbVersions(List<CdrVersionVO> localVersions);

  default List<DbCdrVersion> cdrVersions(CdrConfigVO cdrConfig) {
    return toDbVersions(cdrConfig.cdrVersions);
  }
}
