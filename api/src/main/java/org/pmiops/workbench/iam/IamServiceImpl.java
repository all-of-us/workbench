package org.pmiops.workbench.iam;

import static org.pmiops.workbench.iam.SamConfig.SAM_END_USER_GOOGLE_API;

import com.google.api.services.iam.v1.model.Binding;
import com.google.api.services.iam.v1.model.Policy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudIamClient;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.sam.api.GoogleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class IamServiceImpl implements IamService {

  private final SamApiClientFactory samApiClientFactory;
  private final CloudIamClient cloudIamClient;
  private final CloudResourceManagerService cloudResourceManagerService;
  private final Provider<GoogleApi> endUseGoogleApiProvider;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public IamServiceImpl(
      SamApiClientFactory samApiClientFactory,
      CloudIamClient cloudIamClient,
      CloudResourceManagerService cloudResourceManagerService,
      @Qualifier(SAM_END_USER_GOOGLE_API) Provider<GoogleApi> endUseGoogleApiProvider,
      SamRetryHandler samRetryHandler) {
    this.samApiClientFactory = samApiClientFactory;
    this.cloudIamClient = cloudIamClient;
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.endUseGoogleApiProvider = endUseGoogleApiProvider;
    this.samRetryHandler = samRetryHandler;
  }

  @Override
  public void grantWorkflowRunnerRole(String googleProject) {
    String petServiceAccountName =
        getOrCreatePetServiceAccount(googleProject, endUseGoogleApiProvider.get());
    grantServiceAccountUserRole(googleProject, petServiceAccountName);
    grantLifeScienceRunnerRole(googleProject, petServiceAccountName);
  }

  @Override
  public void grantWorkflowRunnerRoleAsService(String googleProject, String userEmail) {
    GoogleApi googleApiAsImpersonatedUser = new GoogleApi();
    try {
      googleApiAsImpersonatedUser.setApiClient(
          samApiClientFactory.newImpersonatedApiClient(userEmail));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    String petServiceAccountName =
        getOrCreatePetServiceAccount(googleProject, googleApiAsImpersonatedUser);
    grantServiceAccountUserRole(googleProject, petServiceAccountName);
    grantLifeScienceRunnerRole(googleProject, petServiceAccountName);
  }

  /** Gets a Terra pet service account from SAM. SAM will create one if user does not have it. */
  private String getOrCreatePetServiceAccount(String googleProject, GoogleApi googleApi) {
    return samRetryHandler.run(
        (context) -> {
          return googleApi.getPetServiceAccount(googleProject);
        });
  }

  private void grantServiceAccountUserRole(String googleProject, String petServiceAccount) {
    Policy policy = cloudIamClient.getServiceAccountIamPolicy(googleProject, petServiceAccount);
    final String serviceAccountUserRole = "roles/iam.serviceAccountUser";
    List<Binding> bindingList = Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    bindingList.add(
        new Binding()
            .setRole(serviceAccountUserRole)
            .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
    cloudIamClient.setServiceAccountIamPolicy(
        googleProject, petServiceAccount, policy.setBindings(bindingList));
  }

  private void grantLifeScienceRunnerRole(String googleProject, String petServiceAccount) {
    com.google.api.services.cloudresourcemanager.model.Policy policy =
        cloudResourceManagerService.getIamPolicy(googleProject);
    final String lifescienceRunnerRole = "roles/lifesciences.workflowsRunner";
    List<com.google.api.services.cloudresourcemanager.model.Binding> bindingList =
        Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    bindingList.add(
        new com.google.api.services.cloudresourcemanager.model.Binding()
            .setRole(lifescienceRunnerRole)
            .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
    cloudResourceManagerService.setIamPolicy(googleProject, policy.setBindings(bindingList));
  }
}
