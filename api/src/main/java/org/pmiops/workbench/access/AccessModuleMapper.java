package org.pmiops.workbench.access;

import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface AccessModuleMapper {
  @ValueMapping(source = "COMPLIANCE_TRAINING", target = "RT_COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LINK_LOGIN_GOV", target = "RAS_LOGIN_GOV")
  AccessModuleName clientAccessModuleToStorage(AccessModule s);

  @ValueMapping(source = "RT_COMPLIANCE_TRAINING", target = "COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LOGIN_GOV", target = "RAS_LINK_LOGIN_GOV")
AccessModule storageAccessModuleToClient(AccessModuleName s);
}
