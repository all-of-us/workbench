package org.pmiops.workbench.access;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessTierServiceImpl implements AccessTierService {

  private final Provider<WorkbenchConfig> configProvider;

  private final AccessTierDao accessTierDao;

  @Autowired
  public AccessTierServiceImpl(
      Provider<WorkbenchConfig> configProvider, AccessTierDao accessTierDao) {
    this.configProvider = configProvider;
    this.accessTierDao = accessTierDao;
  }

  /**
   * A placeholder implementation until we complete the Controlled Tier access modules.
   *
   * <p>For registered users, return the registered tier or all tiers if we're in an environment
   * which has enabled all tiers for registered users. Return no access tiers for unregistered
   * users.
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment
   */
  public List<DbAccessTier> getAccessTiersForUser(DbUser user) {
    if (user.getDataAccessLevelEnum() == DataAccessLevel.REGISTERED) {
      if (configProvider.get().featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers) {
        return accessTierDao.findAll();
      } else {
        return ImmutableList.of(accessTierDao.findOneByShortName(REGISTERED_TIER_SHORT_NAME));
      }
    } else {
      return Collections.emptyList();
    }
  }
}
