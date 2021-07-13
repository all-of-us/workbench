package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class},
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface UserAccessModuleMapper {
  default List<AccessModuleStatus> dbToModule(
      List<DbUserAccessModule> dbUserAccessModules) {
    return accessModulesToModel(dbUserAccessModules);
  }

  List<AccessModuleStatus> accessModulesToModel(
      List<DbUserAccessModule> dbUserAccessModules);

  @Mapping(target = "accessTierShortName", source = "accessTier.shortName")
  AccessModuleStatus accessModuleToModel(DbUserAccessModule source);
}
