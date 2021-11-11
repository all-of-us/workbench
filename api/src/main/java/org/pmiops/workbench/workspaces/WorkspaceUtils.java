package org.pmiops.workbench.workspaces;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.BillingAccountType;

/** Utility methods for parsing the workspace. */
public class WorkspaceUtils {
  private WorkspaceUtils() {}

  /** Returns {@code true} if the given billing account is workbench free tier billing account. */
  public static boolean isFreeTier(String billingAccountName, WorkbenchConfig workbenchConfig) {
    return billingAccountName.equals(workbenchConfig.billing.freeTierBillingAccountName());
  }

  /** Returns {@code BillingAccountType} by given billing account name. */
  public static BillingAccountType getBillingAccountType(
      String billingAccountName, WorkbenchConfig workbenchConfig) {
    return isFreeTier(billingAccountName, workbenchConfig)
        ? BillingAccountType.FREE_TIER
        : BillingAccountType.USER_PROVIDED;
  }
}
