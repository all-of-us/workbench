package org.pmiops.workbench.billing;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.BillingAccount;
import java.io.IOException;
import javax.inject.Provider;
import org.pmiops.workbench.google.GoogleRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloudBillingClientImpl implements CloudBillingClient {
  private final Provider<Cloudbilling> endUserCloudBillingProvider;
  private final GoogleRetryHandler retryHandler;

  @Autowired
  public CloudBillingClientImpl(
      Provider<Cloudbilling> endUserCloudBillingProvider, GoogleRetryHandler retryHandler) {
    this.endUserCloudBillingProvider = endUserCloudBillingProvider;
    this.retryHandler = retryHandler;
  }

  public BillingAccount getBillingAccount(String billingAccountName) throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) -> {
          return endUserCloudBillingProvider
              .get()
              .billingAccounts()
              .get(billingAccountName)
              .execute();
        });
  }
}
