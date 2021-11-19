package org.pmiops.workbench.google;

import static org.pmiops.workbench.google.GoogleConfig.SERVICE_ACCOUNT_CLOUD_IAM;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.Policy;
import com.google.api.services.iam.v1.model.SetIamPolicyRequest;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
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
  public CloudIamClientImpl(
      @Qualifier(SERVICE_ACCOUNT_CLOUD_IAM) Provider<Iam> iamServiceProvider) {
    this.iamServiceProvider = iamServiceProvider;
  }

  @Override
  public Policy getServiceAccountIamPolicy(String projectId, String serviceAccountName) throws IOException {
    return iamServiceProvider
        .get()
        .projects()
        .serviceAccounts()
        .getIamPolicy(ServiceAccountName.format(projectId, serviceAccountName))
        .execute();
  }

  @Override
  public Policy setServiceAccountIamPolicy(String projectId, String serviceAccountName, Policy policy)
      throws IOException {
    System.out.println("~~~~~~~~setServiceAccountIamPolicy");
    System.out.println("~~~~~~~~setServiceAccountIamPolicy");
    System.out.println("~~~~~~~~");
    System.out.println(policy);
    return iamServiceProvider
        .get()
        .projects()
        .serviceAccounts()
        .setIamPolicy(ServiceAccountName.format(projectId, serviceAccountName), new SetIamPolicyRequest().setPolicy(policy))
        .execute();
  }
}
