package org.pmiops.workbench.google;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudBillingClient {
  /**
   * Poll GCP project's billing info until matches the expected billing account name.
   *
   * <p>This is needed for two reasons. First, we use Terra to update billing accounts, and in
   * Terra's model, a billing project can be associated to many workspaces. So it is an asynchronous
   * process in Terra. Terra has background job that runs every 5s to update billing account for all
   * workspaces. We have to poll this from Google to verify the operation succeeds. Second, Google
   * IAM permissions can take several minutes to propagate throughout the system, so we need to wait
   * for access to the billing info endpoint.
   */
  ProjectBillingInfo pollUntilBillingAccountLinked(String projectId, String billingAccountName)
      throws IOException, InterruptedException;

  /** Get a project's billing account info. */
  ProjectBillingInfo getProjectBillingInfo(String projectId) throws IOException;
}
