package org.pmiops.workbench.google;

import static org.pmiops.workbench.google.GoogleConfig.END_USER_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CloudBillingClientImpl implements CloudBillingClient {
  private final Provider<Cloudbilling> endUserCloudBillingProvider;
  private final GoogleRetryHandler retryHandler;

  @Autowired
  public CloudBillingClientImpl(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> endUserCloudBillingProvider,
      GoogleRetryHandler retryHandler) {
    this.endUserCloudBillingProvider = endUserCloudBillingProvider;
    this.retryHandler = retryHandler;
  }

  @Override
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

  @Override
  public boolean pollUntilBillingAccountLinked(String billingAccountName, String projectId)
      throws IOException, InterruptedException {
    Instant deadline = Instant.now().plusSeconds(30);
    Duration pollingInterval = Duration.ofSeconds(2);
    while (!getProjectBillingInfo(billingAccountName).stream()
        .anyMatch(b -> b.getProjectId() == projectId)) {
      if (Instant.now().plus(pollingInterval).isAfter(deadline)) {
        throw new InterruptedException(
            String.format(
                "Timeout during poll billing account %s for project %s",
                billingAccountName, projectId));
      }
      Thread.sleep(pollingInterval.toMillis());
    }
    return true;
  }

  private List<ProjectBillingInfo> getProjectBillingInfo(String billingAccountName)
      throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) -> {
          return endUserCloudBillingProvider
              .get()
              .billingAccounts()
              .projects()
              .list(billingAccountName)
              .execute()
              .getProjectBillingInfo();
        });
  }
}
