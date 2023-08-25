package org.pmiops.workbench.profile;

import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.GeneralDiscoverySource;
import org.pmiops.workbench.model.PartnerDiscoverySource;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface DiscoverySourceMapper {
  Set<DbUser.DbGeneralDiscoverySource> toDbGeneralDiscoverySources(
      List<GeneralDiscoverySource> generalDiscoverySources);

  Set<DbUser.DbPartnerDiscoverySource> toDbPartnerDiscoverySources(
      List<PartnerDiscoverySource> partnerDiscoverySources);
}
