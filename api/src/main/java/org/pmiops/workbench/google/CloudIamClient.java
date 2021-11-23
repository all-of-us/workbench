package org.pmiops.workbench.google;

import com.google.api.services.iam.v1.model.Policy;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudIamClient {
  /** Gets {@link Policy} for a service account */
  Policy getServiceAccountIamPolicy(String projectId, String serviceAccountName);

  /** Sets a {@link Policy} for a service account. */
  Policy setServiceAccountIamPolicy(String projectId, String serviceAccountName, Policy policy);
}
