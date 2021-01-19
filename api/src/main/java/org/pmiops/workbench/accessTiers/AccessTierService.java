package org.pmiops.workbench.accessTiers;

import java.util.Locale;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessTierService {
  @Autowired private AccessTierDao accessTierDao;

  // FOR PROTOTYPE ONLY
  public DbAccessTier getTierForPrototype(String workspaceName) {
    final String accessTierName =
        workspaceName.toLowerCase(Locale.US).contains("tier2") ? "tier2" : "registered";
    return accessTierDao.findOneByShortName(accessTierName);
  }
}
