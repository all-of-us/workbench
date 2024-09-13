package org.pmiops.workbench.utils;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.BillingAccountType;

public class BillingUtils {
  private BillingUtils() {}

  public static final String BILLING_ACCOUNT_PREFIX = "billingAccounts";

  public static String fullBillingAccountName(String billingAccount) {
    return BILLING_ACCOUNT_PREFIX + "/" + billingAccount;
  }

  /**
   * Returns {@code true} if the given billing account is workbench initial credits billing account.
   */
  public static boolean isInitialCredits(
      String billingAccountName, WorkbenchConfig workbenchConfig) {
    return workbenchConfig.billing.initialCreditsBillingAccountName().equals(billingAccountName);
  }

  /** Returns {@code BillingAccountType} by given billing account name. */
  public static BillingAccountType getBillingAccountType(
      String billingAccountName, WorkbenchConfig workbenchConfig) {
    return isInitialCredits(billingAccountName, workbenchConfig)
        ? BillingAccountType.FREE_TIER
        : BillingAccountType.USER_PROVIDED;
  }
}
