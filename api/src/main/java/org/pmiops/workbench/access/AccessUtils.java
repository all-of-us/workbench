package org.pmiops.workbench.access;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.exceptions.NotFoundException;

/** Utilities for RW Access related functionalities. */
public class AccessUtils {
  private AccessUtils() {}

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
  // see also: AccessModuleServiceImpl.isModuleEnabledInEnvironment()
  static final List<DbAccessModuleName> REQUIRED_MODULES_FOR_REGISTERED_TIER =
      ImmutableList.of(
          DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT,
          DbAccessModuleName.PROFILE_CONFIRMATION,
          DbAccessModuleName.PUBLICATION_CONFIRMATION,
          DbAccessModuleName.IDENTITY,
          DbAccessModuleName.RAS_LOGIN_GOV,
          DbAccessModuleName.RT_COMPLIANCE_TRAINING,
          DbAccessModuleName.TWO_FACTOR_AUTH);

  public static Collection<DbAccessModuleName> getRequiredModulesForRegisteredTierAccess() {
    return REQUIRED_MODULES_FOR_REGISTERED_TIER;
  }

  public static Collection<DbAccessModuleName> getRequiredModulesForRegisteredTierRenewal() {
    return REQUIRED_MODULES_FOR_REGISTERED_TIER;
  }

  static final Collection<DbAccessModuleName> EXCLUSIVE_MODULES_FOR_CONTROLLED_TIER =
      Collections.singleton(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

  public static Collection<DbAccessModuleName> getRequiredModulesForControlledTierAccess() {
    Collection<DbAccessModuleName> required = new ArrayList<>();
    required.addAll(REQUIRED_MODULES_FOR_REGISTERED_TIER);
    required.addAll(EXCLUSIVE_MODULES_FOR_CONTROLLED_TIER);
    return required;
  }

  public static Collection<DbAccessModuleName> getRequiredModulesForControlledTierRenewal() {
    return EXCLUSIVE_MODULES_FOR_CONTROLLED_TIER;
  }
}
