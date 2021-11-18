package org.pmiops.workbench.google;

import com.google.api.services.iam.v1.model.Policy;
import java.io.IOException;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudIamClient {
  /** Gets {@link Policy} for a service account */
  Policy getServiceAccountIamPolicy(String serviceAccountName) throws IOException;

  /** Sets a {@link Policy} for a service account. */
  Policy setServiceAccountIamPolicy(String serviceAccountName, Policy policy) throws IOException;
}
