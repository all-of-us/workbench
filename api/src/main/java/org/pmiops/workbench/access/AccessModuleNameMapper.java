package org.pmiops.workbench.access;

import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface AccessModuleNameMapper {
  @ValueMapping(source = "COMPLIANCE_TRAINING", target = "RT_COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LINK_LOGIN_GOV", target = "RAS_LOGIN_GOV")
  @ValueMapping(source = "RAS_LINK_ID_ME", target = "RAS_ID_ME")
  DbAccessModuleName clientAccessModuleToStorage(AccessModule source);

  @ValueMapping(source = "RT_COMPLIANCE_TRAINING", target = "COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LOGIN_GOV", target = "RAS_LINK_LOGIN_GOV")
  @ValueMapping(source = "RAS_ID_ME", target = "RAS_LINK_ID_ME")
  AccessModule storageAccessModuleToClient(DbAccessModuleName source);

  BypassTimeTargetProperty bypassAuditPropertyFromStorage(DbAccessModuleName source);
}
