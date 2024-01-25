package org.pmiops.workbench.iam;

import com.google.api.services.cloudresourcemanager.v3.model.Policy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.GoogleApi;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.sam.SamApiClientFactory;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IamServiceImpl implements IamService {
  private static final String LIFESCIENCE_RUNNER_ROLE = "roles/lifesciences.workflowsRunner";

  private final CloudResourceManagerService cloudResourceManagerService;
  private final SamApiClientFactory samApiClientFactory;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public IamServiceImpl(
      CloudResourceManagerService cloudResourceManagerService,
      SamApiClientFactory samApiClientFactory,
      SamRetryHandler samRetryHandler) {
    this.cloudResourceManagerService = cloudResourceManagerService;
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
    var samClient = samApiClientFactory.newImpersonatedApiClient(userEmail);
    var tosResult =
        samRetryHandler.run(
            context -> new TermsOfServiceApi(samClient).userTermsOfServiceGetSelf());

    if (tosResult.getPermitsSystemUsage()) {
      return Optional.of(getOrCreatePetServiceAccount(googleProject, new GoogleApi(samClient)));
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
    Policy policy = cloudResourceManagerService.getIamPolicy(googleProject);

    Stream.ofNullable(policy.getBindings())
        .flatMap(List::stream)
        .filter(b -> LIFESCIENCE_RUNNER_ROLE.equals(b.getRole()))
        .findFirst()
        .ifPresent(
            binding ->
                // update the Binding inside the Policy in-place
                binding
                    .getMembers()
                    .removeAll(
                        petServiceAccountsLostAccess.stream()
                            .map(s -> "serviceAccount:" + s)
                            .collect(Collectors.toSet())));

    cloudResourceManagerService.setIamPolicy(googleProject, policy);
  }
}
