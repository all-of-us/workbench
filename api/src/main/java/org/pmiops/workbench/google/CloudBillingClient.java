package org.pmiops.workbench.google;

import com.google.api.services.cloudbilling.model.BillingAccount;
import java.io.IOException;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
public interface CloudBillingClient {
  BillingAccount getBillingAccount(String billingAccount) throws IOException;
}
