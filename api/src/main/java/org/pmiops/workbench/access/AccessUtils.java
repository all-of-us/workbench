package org.pmiops.workbench.access;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessTier;

/** Utilities for RW Access related functionalities. */
public class AccessUtils {
  private AccessUtils() {}
  /** Returns the {@link DbAccessTier} from list of access tiers by tier short name. */
  public static Optional<DbAccessTier> getAccessTierByShortName(
      List<DbAccessTier> accessTierList, String accessTierShortName) {
    return accessTierList.stream()
        .filter(a -> a.getShortName().equals(accessTierShortName))
        .findFirst();
  }

  /** Returns the {@link DbAccessTier} from list of access tiers by tier id. */
  public static Optional<DbAccessTier> getAccessTierById(
      List<DbAccessTier> accessTierList, long accessTierId) {
    return accessTierList.stream().filter(a -> a.getAccessTierId() == accessTierId).findFirst();
  }
}
