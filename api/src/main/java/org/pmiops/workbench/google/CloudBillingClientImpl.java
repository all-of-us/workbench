package org.pmiops.workbench.google;

import static org.pmiops.workbench.google.GoogleConfig.END_USER_CLOUD_BILLING;
import static org.pmiops.workbench.google.GoogleConfig.SERVICE_ACCOUNT_CLOUD_BILLING;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.inject.Provider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CloudBillingClientImpl implements CloudBillingClient {
  private final Provider<Cloudbilling> endUserCloudBillingProvider;
  private final Provider<Cloudbilling> serviceAccountCloudBillingProvider;
  private final GoogleRetryHandler retryHandler;

  @Autowired
  public CloudBillingClientImpl(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> endUserCloudBillingProvider,
      @Qualifier(SERVICE_ACCOUNT_CLOUD_BILLING)
          Provider<Cloudbilling> serviceAccountCloudBillingProvider,
      GoogleRetryHandler retryHandler) {
    this.endUserCloudBillingProvider = endUserCloudBillingProvider;
    this.serviceAccountCloudBillingProvider = serviceAccountCloudBillingProvider;
    this.retryHandler = retryHandler;
  }

  @Override
  public ProjectBillingInfo pollUntilBillingAccountLinked(
      String projectId, String billingAccountName, boolean serviceAccount)
      throws IOException, InterruptedException {
    Duration pollInterval = Duration.ofSeconds(15);
    for (Instant deadline = Instant.now().plusSeconds(300);
        Instant.now().isBefore(deadline);
        Thread.sleep(pollInterval.toMillis())) {
      try {
        ProjectBillingInfo projectBillingInfo =
            serviceAccount
                ? getProjectBillingInfoAsService(projectId)
                : getProjectBillingInfo(projectId);
        if (projectBillingInfo.getBillingAccountName().equals(billingAccountName)) {
          return projectBillingInfo;
        }
      } catch (GoogleJsonResponseException e) {
        if (e.getStatusCode() != 403) {
          // Google may return 403 due to IAM delay, keep retrying on 403, and throw all other
          // exceptions.
          throw e;
        }
      }
    }
    throw new InterruptedException(
        String.format(
            "Timeout during poll billing account %s for project %s",
            billingAccountName, projectId));
  }

  @Override
  public ProjectBillingInfo pollUntilBillingAccountLinked(
      String projectId, String billingAccountName) throws IOException, InterruptedException {
    return pollUntilBillingAccountLinked(projectId, billingAccountName, false);
  }

  @Override
  public ProjectBillingInfo getProjectBillingInfo(String projectId) throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) ->
            endUserCloudBillingProvider
                .get()
                .projects()
                .getBillingInfo("projects/" + projectId)
                .execute());
  }

  @Override
  public ProjectBillingInfo getProjectBillingInfoAsService(String projectId) throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) ->
            serviceAccountCloudBillingProvider
                .get()
                .projects()
                .getBillingInfo("projects/" + projectId)
                .execute());
  }
}
