package org.pmiops.workbench.google;

import static org.pmiops.workbench.google.GoogleConfig.END_USER_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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
  public ProjectBillingInfo pollUntilBillingAccountLinked(
      String projectId, String billingAccountName) throws IOException, InterruptedException {
    Duration pollInterval = Duration.ofSeconds(2);
    for (Instant deadline = Instant.now().plusSeconds(180);
        Instant.now().isBefore(deadline);
        Thread.sleep(pollInterval.toMillis())) {
      ProjectBillingInfo projectBillingInfo = getProjectBillingInfo(projectId);
      if (projectBillingInfo.getBillingAccountName().equals(billingAccountName)) {
        return projectBillingInfo;
      }
    }
    throw new InterruptedException(
        String.format(
            "Timeout during poll billing account %s for project %s",
            billingAccountName, projectId));
  }

  @Override
  public ProjectBillingInfo getProjectBillingInfo(String projectId) throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) -> {
          return endUserCloudBillingProvider
              .get()
              .projects()
              .getBillingInfo("projects/" + projectId)
              .execute();
        });
  }
}
