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
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
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

  private final CloudIamClient cloudIamClient;
  private final CloudResourceManagerService cloudResourceManagerService;
  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final SamApiClientFactory samApiClientFactory;
  private final SamRetryHandler samRetryHandler;
  private final Provider<GoogleApi> endUserGoogleApiProvider;

  @Autowired
  public IamServiceImpl(
      CloudIamClient cloudIamClient,
      CloudResourceManagerService cloudResourceManagerService,
      FirecloudApiClientFactory firecloudApiClientFactory,
      SamApiClientFactory samApiClientFactory,
      SamRetryHandler samRetryHandler,
      @Qualifier(SAM_END_USER_GOOGLE_API) Provider<GoogleApi> endUserGoogleApiProvider) {
    this.cloudIamClient = cloudIamClient;
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.samApiClientFactory = samApiClientFactory;
    this.samRetryHandler = samRetryHandler;
    this.endUserGoogleApiProvider = endUserGoogleApiProvider;
  }

  /** Gets a Terra pet service account from SAM. SAM will create one if one does not yet exist. */
  private String getOrCreatePetServiceAccount(String googleProject, GoogleApi googleApi) {
    return samRetryHandler.run((context) -> googleApi.getPetServiceAccount(googleProject));
  }

  /**
   * Gets a Terra pet service account from SAM as the current user. SAM will create one if one does
   * not yet exist.
   */
  private String getOrCreatePetServiceAccountAsCurrentUser(String googleProject) {
    return getOrCreatePetServiceAccount(googleProject, endUserGoogleApiProvider.get());
  }

  /**
   * Gets a Terra pet service account from SAM as the given user using impersonation, if possible.
   *
   * <p>If the user has not yet accepted the latest Terms of Service, impersonation will not be
   * possible, and we will return Empty instead.
   */
  private Optional<String> getOrCreatePetServiceAccountUsingImpersonation(
      String googleProject, String userEmail) throws IOException, ApiException {

    boolean userAcceptedLatestTos =
        Boolean.TRUE.equals(
            new TermsOfServiceApi(firecloudApiClientFactory.newImpersonatedApiClient(userEmail))
                .getTermsOfServiceStatus());

    if (userAcceptedLatestTos) {
      return Optional.of(
          getOrCreatePetServiceAccount(
              googleProject,
              new GoogleApi(samApiClientFactory.newImpersonatedApiClient(userEmail))));
    } else {
      return Optional.empty();
    }
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
  public void grantWorkflowRunnerRoleToCurrentUser(String googleProject) {
    String petServiceAccountName = getOrCreatePetServiceAccountAsCurrentUser(googleProject);
    grantServiceAccountUserRole(googleProject, petServiceAccountName);
    grantLifeScienceRunnerRole(googleProject, Collections.singletonList(petServiceAccountName));
  }

  @Override
  public List<String> grantWorkflowRunnerRoleForUsers(
      String googleProject, List<String> userEmails) {
    List<String> petServiceAccountFailures = new ArrayList<>();

    try {
      List<String> petServiceAccountsToGrantPermission = new ArrayList<>();
      for (String userEmail : userEmails) {
        Optional<String> petSaMaybe =
            getOrCreatePetServiceAccountUsingImpersonation(googleProject, userEmail);
        if (petSaMaybe.isPresent()) {
          petServiceAccountsToGrantPermission.add(petSaMaybe.get());
          grantServiceAccountUserRole(googleProject, petSaMaybe.get());
        } else {
          petServiceAccountFailures.add(userEmail);
        }
      }
      grantLifeScienceRunnerRole(googleProject, petServiceAccountsToGrantPermission);
    } catch (IOException | ApiException e) {
      throw new ServerErrorException(e);
    }

    return petServiceAccountFailures;
  }

  @Override
  public List<String> revokeWorkflowRunnerRoleForUsers(
      String googleProject, List<String> userEmails) {
    List<String> petServiceAccountFailures = new ArrayList<>();

    try {
      List<String> petServiceAccountsToRevokePermission = new ArrayList<>();
      for (String userEmail : userEmails) {
        Optional<String> petSaMaybe =
            getOrCreatePetServiceAccountUsingImpersonation(googleProject, userEmail);
        if (petSaMaybe.isPresent()) {
          petServiceAccountsToRevokePermission.add(petSaMaybe.get());
        } else {
          petServiceAccountFailures.add(userEmail);
        }
      }
      revokeLifeScienceRunnerRole(googleProject, petServiceAccountsToRevokePermission);
    } catch (IOException | ApiException e) {
      throw new ServerErrorException(e);
    }

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
