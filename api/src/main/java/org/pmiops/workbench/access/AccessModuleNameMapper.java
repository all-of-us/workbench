package org.pmiops.workbench.access;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService.BadgeName;
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

  // only compliance training modules have associated badges
  @ValueMapping(source = "RT_COMPLIANCE_TRAINING", target = "REGISTERED_TIER_TRAINING")
  @ValueMapping(source = "CT_COMPLIANCE_TRAINING", target = "CONTROLLED_TIER_TRAINING")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
  BadgeName badgeFromModule(DbAccessModuleName source);

  @ValueMapping(source = "REGISTERED_TIER_TRAINING", target = "RT_COMPLIANCE_TRAINING")
  @ValueMapping(source = "CONTROLLED_TIER_TRAINING", target = "CT_COMPLIANCE_TRAINING")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
  DbAccessModuleName moduleFromBadge(BadgeName source);
}
