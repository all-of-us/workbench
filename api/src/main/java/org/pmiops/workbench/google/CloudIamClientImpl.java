package org.pmiops.workbench.google;

import static org.pmiops.workbench.google.GoogleConfig.END_USER_CLOUD_BILLING;
import static org.pmiops.workbench.google.GoogleConfig.SERVICE_ACCOUNT_CLOUD_IAM;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.Policy;
import com.google.api.services.iam.v1.model.SetIamPolicyRequest;
import java.io.IOException;
import javax.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Encapsulate Google APIs for interfacing with Google Cloud IAM APIs. */
@Service
public class CloudIamClientImpl implements CloudIamClient {
  private final Provider<Iam> iamServiceProvider;

  @Autowired
  public CloudIamClientImpl(@Qualifier(SERVICE_ACCOUNT_CLOUD_IAM) Provider<Iam> iamServiceProvider) {
    this.iamServiceProvider = iamServiceProvider;
  }

  @Override
  public Policy getServiceAccountIamPolicy(String serviceAccountName) throws IOException {
    return iamServiceProvider
        .get()
        .projects()
        .serviceAccounts()
        .getIamPolicy(serviceAccountName)
        .execute();
  }

  @Override
  public Policy setServiceAccountIamPolicy(String serviceAccountName, Policy policy)
      throws IOException {
    return iamServiceProvider
        .get()
        .projects()
        .serviceAccounts()
        .setIamPolicy(serviceAccountName, new SetIamPolicyRequest().setPolicy(policy))
        .execute();
  }

  /**
   * Returns the service account email based on the project id and the service account id. The
   * service account id is the "username" of the service account email.
   */
  public static String emailFromAccountId(String accountId, String projectId) {
    return String.format("%s@%s.iam.gserviceaccount.com", accountId, projectId);
  }
}
