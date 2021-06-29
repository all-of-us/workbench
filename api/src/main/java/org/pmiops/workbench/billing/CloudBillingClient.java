package org.pmiops.workbench.billing;

import com.google.api.services.cloudbilling.model.BillingAccount;
import java.io.IOException;
import org.springframework.stereotype.Service;

/** Encapsulate Google APIs for interfacing with Google Cloud Billing APIs. */
@Service
public interface CloudBillingClient {
  BillingAccount getBillingAccount(String billingAccount) throws IOException;
}
