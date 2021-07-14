package org.pmiops.workbench.access;

import java.util.List;
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
}
