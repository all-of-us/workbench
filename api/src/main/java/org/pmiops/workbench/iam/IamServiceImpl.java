package org.pmiops.workbench.iam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.api.GoogleApi;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.sam.SamApiClientFactory;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IamServiceImpl implements IamService {

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
}
