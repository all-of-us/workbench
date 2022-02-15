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
public interface AccessModuleMapper {
  @ValueMapping(source = "COMPLIANCE_TRAINING", target = "RT_COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LINK_LOGIN_GOV", target = "RAS_LOGIN_GOV")
  DbAccessModuleName clientAccessModuleToStorage(AccessModule source);

  @ValueMapping(source = "RT_COMPLIANCE_TRAINING", target = "COMPLIANCE_TRAINING")
  @ValueMapping(source = "RAS_LOGIN_GOV", target = "RAS_LINK_LOGIN_GOV")
  AccessModule storageAccessModuleToClient(DbAccessModuleName source);

  // these modules cannot be bypassed
  @ValueMapping(source = "PROFILE_CONFIRMATION", target = MappingConstants.NULL)
  @ValueMapping(source = "PUBLICATION_CONFIRMATION", target = MappingConstants.NULL)
  BypassTimeTargetProperty auditAccessModuleFromStorage(DbAccessModuleName source);

  /*
  > Task :compileJava
/Users/thibault/src/aou/api/src/main/java/org/pmiops/workbench/access/AccessModuleMapper.java:27: error: The following constants from the source enum have no corresponding constant in the target enum and must be be mapped via adding additional mappings: ERA_COMMONS, TWO_FACTOR_AUTH, RAS_LOGIN_GOV, RT_COMPLIANCE_TRAINING, DATA_USER_CODE_OF_CONDUCT, PROFILE_CONFIRMATION, PUBLICATION_CONFIRMATION, CT_COMPLIANCE_TRAINING.
  BadgeName badgeFromStorage(DbAccessModuleName source);


accessmodulemappertest
   */
  BadgeName badgeFromStorage(DbAccessModuleName source);
}
