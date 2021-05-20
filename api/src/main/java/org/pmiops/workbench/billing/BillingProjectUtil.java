package org.pmiops.workbench.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;

/** Utilities for Billing Project related methods. */
public class BillingProjectUtil {
  private BillingProjectUtil() {}
  @VisibleForTesting
  static final int PROJECT_BILLING_ID_SIZE = 8;

  /** Creates a random Billing Project name. */
  public static String createBillingProjectName(String projectNamePrefix) {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PROJECT_BILLING_ID_SIZE);

    if (!projectNamePrefix.endsWith("-")) {
      projectNamePrefix = projectNamePrefix + "-";
    }

    return projectNamePrefix + randomString;
  }
}
