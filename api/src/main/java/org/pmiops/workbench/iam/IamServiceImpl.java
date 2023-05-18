package org.pmiops.workbench.iam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.api.GoogleApi;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IamServiceImpl implements IamService {
  private static final String LIFESCIENCE_RUNNER_ROLE = "roles/lifesciences.workflowsRunner";

  private final CloudResourceManagerService cloudResourceManagerService;
  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final SamApiClientFactory samApiClientFactory;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public IamServiceImpl(
      CloudResourceManagerService cloudResourceManagerService,
      FirecloudApiClientFactory firecloudApiClientFactory,
      SamApiClientFactory samApiClientFactory,
      SamRetryHandler samRetryHandler) {
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.samApiClientFactory = samApiClientFactory;
    this.samRetryHandler = samRetryHandler;
  }

  /** Gets a Terra pet service account from SAM. SAM will create one if one does not yet exist. */
  private String getOrCreatePetServiceAccount(String googleProject, GoogleApi googleApi) {
    return samRetryHandler.run((context) -> googleApi.getPetServiceAccount(googleProject));
  }

  /**
   * Gets a Terra pet service account from SAM as the given user using impersonation, if possible.
   *
   * <p>If the user has not yet accepted the latest Terms of Service, impersonation will not be
   * possible, and we will return Empty instead.
   */
  @Override
  public Optional<String> getOrCreatePetServiceAccountUsingImpersonation(
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

  @Override
  public List<String> revokeWorkflowRunnerRoleForUsers(
      String googleProject, List<String> userEmails) {
    List<String> petServiceAccountFailures = new ArrayList<>();

    try {
      List<String> petServiceAccountsToRevokePermission = new ArrayList<>();
      for (String userEmail : userEmails) {
        // TODO can we make these requests in parallel?
        getOrCreatePetServiceAccountUsingImpersonation(googleProject, userEmail)
            .ifPresentOrElse(
                petServiceAccountsToRevokePermission::add,
                () -> petServiceAccountFailures.add(userEmail));
      }
      revokeLifeScienceRunnerRole(googleProject, petServiceAccountsToRevokePermission);
    } catch (IOException | ApiException e) {
      throw new ServerErrorException(e);
    }

    return petServiceAccountFailures;
  }

  /** Revokes life science runner role to list of service accounts. */
  private void revokeLifeScienceRunnerRole(
      String googleProject, List<String> petServiceAccountsLostAccess) {
    com.google.api.services.cloudresourcemanager.v3.model.Policy policy =
        cloudResourceManagerService.getIamPolicy(googleProject);
    List<com.google.api.services.cloudresourcemanager.v3.model.Binding> bindingList =
        Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
    com.google.api.services.cloudresourcemanager.v3.model.Binding binding =
        bindingList.stream()
            .filter(b -> LIFESCIENCE_RUNNER_ROLE.equals(b.getRole()))
            .findFirst()
            .orElse(
                new com.google.api.services.cloudresourcemanager.v3.model.Binding()
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
