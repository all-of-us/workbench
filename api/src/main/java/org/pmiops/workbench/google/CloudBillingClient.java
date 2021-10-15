package org.pmiops.workbench.google;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudBillingClient {
  /**
   * Poll GCP project's billing info until matches the expected billing account name.
   *
   * <p>This is needed because we uses Terra to update billing accounts, and in Terra's model, a
   * billing project can be assosicated to many workspaces. So it is an asynchronous process in
   * Terra. Terra has backgroun job that runs every 5s to update billing account for all workspaces.
   * We have to poll this from Google to verify the operatiohn succeeds.
   */
  ProjectBillingInfo pollUntilBillingAccountLinked(String projectId, String billingAccountName)
      throws IOException, InterruptedException;

  /** Get a project's billing accont info. */
  ProjectBillingInfo getProjectBillingInfo(String projectId) throws IOException;
}
