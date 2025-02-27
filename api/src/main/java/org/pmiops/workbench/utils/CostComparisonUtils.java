package org.pmiops.workbench.utils;

import com.google.common.math.DoubleMath;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbUser;

public class CostComparisonUtils {

  // somewhat arbitrary - 1 per million
  private static final double COST_COMPARISON_TOLERANCE = 0.000001;
  private static final double COST_FRACTION_TOLERANCE = 0.000001;

  private CostComparisonUtils() {}

  public static int compareCosts(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_COMPARISON_TOLERANCE);
  }

  public static boolean costsDiffer(final double a, final double b) {
    return compareCosts(a, b) != 0;
  }

  public static int compareCostFractions(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_FRACTION_TOLERANCE);
  }

  public static boolean costAboveLimit(
      final DbUser user, final double currentCost, Double defaultInitialCreditsLimit) {
    return compareCosts(currentCost, getUserInitialCreditsLimit(user, defaultInitialCreditsLimit))
        > 0;
  }

  /**
   * Retrieve the intial credits limit actually applicable to this user: this user's override if
   * present, the environment's default if not
   *
   * @param user the user as represented in our database
   * @param defaultInitialCreditsLimit
   * @return the US dollar amount, represented as a double
   */
  public static double getUserInitialCreditsLimit(DbUser user, Double defaultInitialCreditsLimit) {
    return Optional.ofNullable(user.getInitialCreditsLimitOverride())
        .orElse(defaultInitialCreditsLimit);
  }
}
