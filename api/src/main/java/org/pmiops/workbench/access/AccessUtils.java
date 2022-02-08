package org.pmiops.workbench.access;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AccessModule;

/** Utilities for RW Access related functionalities. */
public class AccessUtils {
  private AccessUtils() {}

  private static final BiMap<AccessModule, AccessModuleName> CLIENT_TO_STORAGE_ACCESS_MODULE =
      ImmutableBiMap.<AccessModule, AccessModuleName>builder()
          .put(AccessModule.TWO_FACTOR_AUTH, AccessModuleName.TWO_FACTOR_AUTH)
          .put(AccessModule.ERA_COMMONS, AccessModuleName.ERA_COMMONS)
          .put(AccessModule.COMPLIANCE_TRAINING, AccessModuleName.RT_COMPLIANCE_TRAINING)
          .put(AccessModule.CT_COMPLIANCE_TRAINING, AccessModuleName.CT_COMPLIANCE_TRAINING)
          .put(AccessModule.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
          .put(AccessModule.DATA_USER_CODE_OF_CONDUCT, AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
          .put(AccessModule.PUBLICATION_CONFIRMATION, AccessModuleName.PUBLICATION_CONFIRMATION)
          .put(AccessModule.PROFILE_CONFIRMATION, AccessModuleName.PROFILE_CONFIRMATION)
          .build();

  private static final BiMap<BypassTimeTargetProperty, AccessModuleName>
      AUDIT_TO_STORAGE_ACCESS_MODULE =
          ImmutableBiMap.<BypassTimeTargetProperty, AccessModuleName>builder()
              .put(BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME, AccessModuleName.ERA_COMMONS)
              .put(
                  BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
                  AccessModuleName.RT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.CT_COMPLIANCE_TRAINING_BYPASS_TIME,
                  AccessModuleName.CT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
                  AccessModuleName.TWO_FACTOR_AUTH)
              .put(BypassTimeTargetProperty.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
              .put(
                  BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
                  AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
              .build();

  /** Converts {@link AccessModule} to {@link AccessModuleName}. */
  public static AccessModuleName clientAccessModuleToStorage(AccessModule s) {
    return CLIENT_TO_STORAGE_ACCESS_MODULE.get(s);
  }

  /** Converts {@link AccessModuleName} to {@link AccessModule}. */
  public static AccessModule storageAccessModuleToClient(AccessModuleName s) {
    return CLIENT_TO_STORAGE_ACCESS_MODULE.inverse().get(s);
  }

  /** Converts {@link BypassTimeTargetProperty} to {@link AccessModuleName}. */
  public static AccessModuleName auditAccessModuleToStorage(BypassTimeTargetProperty b) {
    return AUDIT_TO_STORAGE_ACCESS_MODULE.get(b);
  }

  /** Converts {@link AccessModuleName} to {@link BypassTimeTargetProperty}. */
  public static BypassTimeTargetProperty auditAccessModuleFromStorage(AccessModuleName s) {
    return AUDIT_TO_STORAGE_ACCESS_MODULE.inverse().get(s);
  }

  /**
   * Returns the {@link DbAccessTier} from list of access tiers by tier short name. Throws {@link
   * NotFoundException} if tier not found.
   */
  public static DbAccessTier getAccessTierByShortNameOrThrow(
      List<DbAccessTier> accessTierList, String accessTierShortName) {
    return accessTierList.stream()
        .filter(a -> a.getShortName().equals(accessTierShortName))
        .findFirst()
        .orElseThrow(
            () -> new NotFoundException("Access tier " + accessTierShortName + "not found"));
  }

  // missing ERA_COMMONS (special-cased)
  // see also: AccessModuleServiceImpl.isModuleRequiredInEnvironment()
  public static final List<AccessModuleName> REQUIRED_MODULES_FOR_REGISTERED_TIER =
      ImmutableList.of(
          AccessModuleName.TWO_FACTOR_AUTH,
          AccessModuleName.RT_COMPLIANCE_TRAINING,
          AccessModuleName.DATA_USER_CODE_OF_CONDUCT,
          AccessModuleName.RAS_LOGIN_GOV,
          AccessModuleName.PROFILE_CONFIRMATION,
          AccessModuleName.PUBLICATION_CONFIRMATION);

  public static final List<AccessModuleName> REQUIRED_MODULES_FOR_CONTROLLED_TIER =
      ImmutableList.of(
          AccessModuleName.TWO_FACTOR_AUTH,
          AccessModuleName.RT_COMPLIANCE_TRAINING,
          AccessModuleName.DATA_USER_CODE_OF_CONDUCT,
          AccessModuleName.RAS_LOGIN_GOV,
          AccessModuleName.PROFILE_CONFIRMATION,
          AccessModuleName.PUBLICATION_CONFIRMATION,
          AccessModuleName.CT_COMPLIANCE_TRAINING);

}
