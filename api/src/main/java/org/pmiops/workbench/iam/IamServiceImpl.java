package org.pmiops.workbench.iam;

import static org.pmiops.workbench.iam.SamConfig.SAM_END_USER_GOOGLE_API;

import com.google.api.services.iam.v1.model.Binding;
import com.google.api.services.iam.v1.model.Policy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.google.CloudIamClient;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.sam.api.GoogleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class IamServiceImpl implements IamService {
  private static final String SERVICE_ACCOUNT_USER_ROLE = "roles/iam.serviceAccountUser";
  private static final String LIFESCIENCE_RUNNER_ROLE = "roles/lifesciences.workflowsRunner";

  private final SamApiClientFactory samApiClientFactory;
  private final CloudIamClient cloudIamClient;
  private final CloudResourceManagerService cloudResourceManagerService;
  private final Provider<GoogleApi> endUserGoogleApiProvider;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public IamServiceImpl(
      SamApiClientFactory samApiClientFactory,
      CloudIamClient cloudIamClient,
      CloudResourceManagerService cloudResourceManagerService,
      @Qualifier(SAM_END_USER_GOOGLE_API) Provider<GoogleApi> endUserGoogleApiProvider,
      SamRetryHandler samRetryHandler) {
    this.samApiClientFactory = samApiClientFactory;
    this.cloudIamClient = cloudIamClient;
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.endUserGoogleApiProvider = endUserGoogleApiProvider;
    this.samRetryHandler = samRetryHandler;
  }

  @Override
  public void grantWorkflowRunnerRoleToCurrentUser(String googleProject) {
    String petServiceAccountName =
        getOrCreatePetServiceAccount(googleProject, endUserGoogleApiProvider.get());
    grantServiceAccountUserRole(googleProject, petServiceAccountName);
    grantLifeScienceRunnerRole(googleProject, Collections.singletonList(petServiceAccountName));
  }

  /** Gets a Terra pet service account from SAM. SAM will create one if one does not yet exist. */
  private String getOrCreatePetServiceAccount(String googleProject, GoogleApi googleApi) {
    return samRetryHandler.run(
        (context) -> {
          return googleApi.getPetServiceAccount(googleProject);
        });
  }

  /**
   * Gets a Terra pet service account from SAM. SAM will create one if one does not yet exist.
   *
   * <p>Differs from the above version in that it uses impersonation.
   */
  private String getOrCreatePetServiceAccountUsingImpersonation(
      String googleProject, String userEmail) throws IOException {
    GoogleApi googleApiAsImpersonatedUser = new GoogleApi();
    googleApiAsImpersonatedUser.setApiClient(
        samApiClientFactory.newImpersonatedApiClient(userEmail));
    return getOrCreatePetServiceAccount(googleProject, googleApiAsImpersonatedUser);
  }

  private void grantServiceAccountUserRole(String googleProject, String petServiceAccount) {
    Policy policy = cloudIamClient.getServiceAccountIamPolicy(googleProject, petServiceAccount);
    List<Binding> bindingList = Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    bindingList.add(
        new Binding()
            .setRole(SERVICE_ACCOUNT_USER_ROLE)
            .setMembers(Collections.singletonList("serviceAccount:" + petServiceAccount)));
    cloudIamClient.setServiceAccountIamPolicy(
        googleProject, petServiceAccount, policy.setBindings(bindingList));
  }

  @Override
  public List<String> grantWorkflowRunnerRoleForUsers(
      String googleProject, List<String> userEmails) {
    List<String> petServiceAccountsToGrantPermission = new ArrayList<>();
    List<String> petServiceAccountFailures = new ArrayList<>();

    for (String userEmail : userEmails) {
      try {
        String petServiceAccountName =
            getOrCreatePetServiceAccountUsingImpersonation(googleProject, userEmail);
        petServiceAccountsToGrantPermission.add(petServiceAccountName);
        grantServiceAccountUserRole(googleProject, petServiceAccountName);
      } catch (Exception e) {
        petServiceAccountFailures.add(userEmail);
      }
    }
    grantLifeScienceRunnerRole(googleProject, petServiceAccountsToGrantPermission);
    return petServiceAccountFailures;
  }

  @Override
  public List<String> revokeWorkflowRunnerRoleForUsers(
      String googleProject, List<String> userEmails) {
    List<String> petServiceAccountsToRevokePermission = new ArrayList<>();
    List<String> petServiceAccountFailures = new ArrayList<>();

    for (String userEmail : userEmails) {
      try {
        String petServiceAccountName =
            getOrCreatePetServiceAccountUsingImpersonation(googleProject, userEmail);
        petServiceAccountsToRevokePermission.add(petServiceAccountName);
      } catch (Exception e) {
        petServiceAccountFailures.add(userEmail);
      }
    }
    revokeLifeScienceRunnerRole(googleProject, petServiceAccountsToRevokePermission);
    return petServiceAccountFailures;
  }

  /** Grants life science runner role to list of service accounts. */
  private void grantLifeScienceRunnerRole(String googleProject, List<String> petServiceAccounts) {
    com.google.api.services.cloudresourcemanager.model.Policy policy =
        cloudResourceManagerService.getIamPolicy(googleProject);
    List<com.google.api.services.cloudresourcemanager.model.Binding> bindingList =
        Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    bindingList.add(
        new com.google.api.services.cloudresourcemanager.model.Binding()
            .setRole(LIFESCIENCE_RUNNER_ROLE)
            .setMembers(
                petServiceAccounts.stream()
                    .map(s -> "serviceAccount:" + s)
                    .collect(Collectors.toList())));
    cloudResourceManagerService.setIamPolicy(googleProject, policy.setBindings(bindingList));
  }

  /** Revokes life science runner role to list of service accounts. */
  private void revokeLifeScienceRunnerRole(
      String googleProject, List<String> petServiceAccountsLostAccess) {
    com.google.api.services.cloudresourcemanager.model.Policy policy =
        cloudResourceManagerService.getIamPolicy(googleProject);
    List<com.google.api.services.cloudresourcemanager.model.Binding> bindingList =
        Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    com.google.api.services.cloudresourcemanager.model.Binding binding =
        bindingList.stream()
            .filter(b -> LIFESCIENCE_RUNNER_ROLE.equals(b.getRole()))
            .findFirst()
            .orElse(
                new com.google.api.services.cloudresourcemanager.model.Binding()
                    .setRole(LIFESCIENCE_RUNNER_ROLE)
                    .setMembers(new ArrayList<>()));

    binding
        .getMembers()
        .removeAll(
            petServiceAccountsLostAccess.stream()
                .map(s -> "serviceAccount:" + s)
                .collect(Collectors.toList()));

    cloudResourceManagerService.setIamPolicy(googleProject, policy);
  }
}
