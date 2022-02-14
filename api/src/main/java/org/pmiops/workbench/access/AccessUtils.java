package org.pmiops.workbench.access;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.exceptions.NotFoundException;

/** Utilities for RW Access related functionalities. */
public class AccessUtils {
  private AccessUtils() {}

  private static final BiMap<BypassTimeTargetProperty, DbAccessModuleName>
      AUDIT_TO_STORAGE_ACCESS_MODULE =
          ImmutableBiMap.<BypassTimeTargetProperty, DbAccessModuleName>builder()
              .put(BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME, DbAccessModuleName.ERA_COMMONS)
              .put(
                  BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
                  DbAccessModuleName.RT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.CT_COMPLIANCE_TRAINING_BYPASS_TIME,
                  DbAccessModuleName.CT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
                  DbAccessModuleName.TWO_FACTOR_AUTH)
              .put(BypassTimeTargetProperty.RAS_LINK_LOGIN_GOV, DbAccessModuleName.RAS_LOGIN_GOV)
              .put(
                  BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
                  DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT)
              .build();

  /** Converts {@link BypassTimeTargetProperty} to {@link DbAccessModuleName}. */
  public static DbAccessModuleName auditAccessModuleToStorage(BypassTimeTargetProperty b) {
    return AUDIT_TO_STORAGE_ACCESS_MODULE.get(b);
  }

  /** Converts {@link DbAccessModuleName} to {@link BypassTimeTargetProperty}. */
  public static BypassTimeTargetProperty auditAccessModuleFromStorage(DbAccessModuleName s) {
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
  public static final List<DbAccessModuleName> REQUIRED_MODULES_FOR_REGISTERED_TIER =
      ImmutableList.of(
          DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT,
          DbAccessModuleName.PROFILE_CONFIRMATION,
          DbAccessModuleName.PUBLICATION_CONFIRMATION,
          DbAccessModuleName.RAS_LOGIN_GOV,
          DbAccessModuleName.RT_COMPLIANCE_TRAINING,
          DbAccessModuleName.TWO_FACTOR_AUTH);

  public static final List<DbAccessModuleName> REQUIRED_MODULES_FOR_CONTROLLED_TIER =
      ImmutableList.of(
          DbAccessModuleName.CT_COMPLIANCE_TRAINING,
          DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT,
          DbAccessModuleName.PROFILE_CONFIRMATION,
          DbAccessModuleName.PUBLICATION_CONFIRMATION,
          DbAccessModuleName.RAS_LOGIN_GOV,
          DbAccessModuleName.RT_COMPLIANCE_TRAINING,
          DbAccessModuleName.TWO_FACTOR_AUTH);
}
