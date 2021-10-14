package org.pmiops.workbench.google;

import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudBillingClient {
  /** Poll GCP project's billing info until matches the expected billing account name.
   * @return*/
  ProjectBillingInfo pollUntilBillingAccountLinked(String billingAccountName, String projectId)
      throws IOException, InterruptedException;

  /** Get a project's billing accont info. */
  ProjectBillingInfo getProjectBillingInfo(String projectId) throws IOException;
}
