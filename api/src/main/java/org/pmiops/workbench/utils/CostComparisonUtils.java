package org.pmiops.workbench.utils;

import com.google.common.math.DoubleMath;

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
}
