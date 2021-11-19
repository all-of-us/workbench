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
import org.pmiops.workbench.sam.ApiException;
import org.pmiops.workbench.sam.api.GoogleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class IamClientImpl implements IamClient {

  private final SamApiClientFactory samApiClientFactory;
  private final CloudIamClient cloudIamClient;
  private final Provider<GoogleApi> endUseGoogleApiProvider;

  @Autowired
  public IamClientImpl(
      SamApiClientFactory samApiClientFactory,
      CloudIamClient cloudIamClient,
      @Qualifier(SAM_END_USER_GOOGLE_API) Provider<GoogleApi> endUseGoogleApiProvider) {
    this.samApiClientFactory = samApiClientFactory;
    this.cloudIamClient = cloudIamClient;
    this.endUseGoogleApiProvider = endUseGoogleApiProvider;
  }

  @Override
  public void grantWorkflowRunnerRole(String googleProject) {
    System.out.println("~~~~~!!!!!!!!");
    System.out.println("grantWorkflowRunnerRole");
    grantServiceAccountUserRole(googleProject, endUseGoogleApiProvider.get());
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
    grantServiceAccountUserRole(googleProject, googleApiAsImpersonatedUser);
  }

  private void grantServiceAccountUserRole(String googleProject, GoogleApi googleApi) {
    try {
      String petServiceAccount = googleApi.getPetServiceAccount(googleProject);
      System.out.println("~~~~~~~petServiceAccount");
      System.out.println(petServiceAccount);
      Policy policy = cloudIamClient.getServiceAccountIamPolicy(googleProject, petServiceAccount);
      final String serviceAccountUserRole = "roles/iam.serviceAccountUser";
      List<Binding> bindingList = Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
      bindingList.add(
          new Binding()
              .setRole(serviceAccountUserRole)
              .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
      cloudIamClient.setServiceAccountIamPolicy(googleProject, petServiceAccount, policy.setBindings(bindingList));
    } catch (IOException | ApiException e) {
      throw new ServerErrorException(e);
    }
  }

  private void grantLifeScienceRunnerRole(String googleProject, GoogleApi googleApi) {
    try {
      String petServiceAccount = googleApi.getPetServiceAccount(googleProject);
      System.out.println("~~~~~~~petServiceAccount");
      System.out.println(petServiceAccount);
      Policy policy = cloudIamClient.getServiceAccountIamPolicy(googleProject, petServiceAccount);
      final String lifescienceRunnerRole = "roles/lifesciences.workflowsRunner";
      final String serviceAccountUserRole = "roles/iam.serviceAccountUser";
      List<Binding> bindingList = Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
      bindingList.add(
          new Binding()
              .setRole(serviceAccountUserRole)
              .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
      bindingList.add(
          new Binding()
              .setRole(lifescienceRunnerRole)
              .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
      cloudIamClient.setServiceAccountIamPolicy(googleProject, petServiceAccount, policy.setBindings(bindingList));
    } catch (IOException | ApiException e) {
      throw new ServerErrorException(e);
    }
  }
}
