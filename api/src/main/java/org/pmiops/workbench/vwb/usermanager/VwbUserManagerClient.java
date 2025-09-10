package org.pmiops.workbench.vwb.usermanager;

import jakarta.inject.Provider;
import java.util.Optional;
import java.util.UUID;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.user.ApiException;
import org.pmiops.workbench.vwb.user.api.OrganizationV2Api;
import org.pmiops.workbench.vwb.user.api.PodApi;
import org.pmiops.workbench.vwb.user.api.UserV2Api;
import org.pmiops.workbench.vwb.user.api.WorkbenchGroupApi;
import org.pmiops.workbench.vwb.user.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VwbUserManagerClient {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserManagerClient.class);

  private final Provider<UserV2Api> userV2ApiProvider;

  private final Provider<OrganizationV2Api> organizationV2ApiProvider;
  private final Provider<WorkbenchGroupApi> groupApiProvider;

  private final VwbUserManagerRetryHandler vwbUserManagerRetryHandler;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final Provider<PodApi> podApiProvider;

  public VwbUserManagerClient(
      @Qualifier(VwbUserManagerConfig.VWB_SERVICE_ACCOUNT_USER_API)
          Provider<UserV2Api> userV2ApiProvider,
      Provider<OrganizationV2Api> organizationV2ApiProvider,
      Provider<WorkbenchGroupApi> groupApiProvider,
      VwbUserManagerRetryHandler vwbUserManagerRetryHandler,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<PodApi> podApiProvider) {
    this.userV2ApiProvider = userV2ApiProvider;
    this.organizationV2ApiProvider = organizationV2ApiProvider;
    this.groupApiProvider = groupApiProvider;
    this.vwbUserManagerRetryHandler = vwbUserManagerRetryHandler;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.podApiProvider = podApiProvider;
  }

  public OrganizationMember getOrganizationMember(String userName) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    return vwbUserManagerRetryHandler.run(
        context ->
            organizationV2ApiProvider.get().getOrganizationMemberV2(organizationId, userName));
  }

  public void createUser(String email) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.info("Creating user in VWB with email {}", email);
    vwbUserManagerRetryHandler.run(
        context ->
            userV2ApiProvider
                .get()
                .createUserV2(new UserCreateRequest().email(email), organizationId));
  }

  /** Adds a user into VWB user group. */
  public void addUserToGroup(String groupName, String email) {
    logger.info("Adding user in VWB group {}, with email {}", groupName, email);
    updateGroupMembership(groupName, email, SetAccessOperation.GRANT);
  }

  /** Removes a user into VWB user group. */
  public void removeUserFromGroup(String groupName, String email) {
    logger.info("Removing user in VWB group {}, with email {}", groupName, email);
    updateGroupMembership(groupName, email, SetAccessOperation.REVOKE);
  }

  private void updateGroupMembership(String groupName, String email, SetAccessOperation operation) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    SetAccessRequest setAccessRequest =
        new SetAccessRequest()
            .role(GroupRole.MEMBER)
            .operation(operation)
            .principal(new Principal().userPrincipal(new PrincipalUser().email(email)));
    vwbUserManagerRetryHandler.run(
        context -> {
          groupApiProvider.get().setGroupAccess(setAccessRequest, groupName, organizationId);
          return null;
        });
  }

  public PodDescription createPodForUserWithEmail(String email) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    String initialCreditBillingAccount = workbenchConfigProvider.get().billing.accountId;
    logger.info("Creating pod for user in VWB with email {}", email);
    String userFacingId = getUserFacingId(email);
    return vwbUserManagerRetryHandler.run(
        context ->
            podApiProvider
                .get()
                .createPod(
                    new PodCreateRequest()
                        .userFacingId(userFacingId)
                        .description("Pod for " + email)
                        .environment(
                            new PodEnvironment()
                                .environmentType(PodEnvironmentType.GCP)
                                .environmentDataGcp(
                                    new PodEnvironmentDataGcp()
                                        .billingAccountId(initialCreditBillingAccount))),
                    organizationId));
  }

  public void sharePodWithUserWithRole(UUID podId, String email, PodRole podRole)
      throws ApiException {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.info("Sharing pod {} with user {} with role {}", podId, email, podRole);
    vwbUserManagerRetryHandler.runAndThrowChecked(
        context -> {
          podApiProvider.get().grantMemberPodRole(organizationId, podId.toString(), email, podRole);
          return null;
        });
  }

  public void deletePod(UUID podId) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.info("Deleting pod {}", podId);
    vwbUserManagerRetryHandler.run(
        context -> {
          podApiProvider
              .get()
              .deletePod(
                  new DeletePodRequest().jobControl(generateJobControlWithUUID()),
                  organizationId,
                  podId.toString());
          return null;
        });
  }

  /**
   * Gets a pod by its ID.
   *
   * @param podId The ID of the pod to retrieve.
   * @return The PodDescription for the specified pod ID.
   */
  public Optional<PodDescription> getPodById(String podId) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.debug("Getting pod by id {}", podId);
    return Optional.ofNullable(
        vwbUserManagerRetryHandler.run(
            context ->
                podApiProvider.get().getPod(organizationId, podId, PodAction.READ_METADATA)));
  }

  /**
   * Unlinks the billing account from a pod.
   *
   * @param podId The ID of the pod to unlink the billing account from.
   */
  public void unlinkBillingAccountFromPod(String podId) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.info("Unlinking billing account from pod {}", podId);
    vwbUserManagerRetryHandler.run(
        context ->
            podApiProvider
                .get()
                .unlinkBillingFromPod(
                    new PodUnlinkBillingRequest().jobControl(generateJobControlWithUUID()),
                    organizationId,
                    podId));
  }

  public void updatePodBillingAccount(String vwbPodId, String billingAccount) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    vwbUserManagerRetryHandler.run(
        context ->
            podApiProvider
                .get()
                .updatePod(
                    new PodUpdateRequest()
                        .jobControl(generateJobControlWithUUID())
                        .environment(
                            new PodEnvironment()
                                .environmentType(PodEnvironmentType.GCP)
                                .environmentDataGcp(
                                    new PodEnvironmentDataGcp().billingAccountId(billingAccount))),
                    organizationId,
                    vwbPodId));
  }

  private static JobControl generateJobControlWithUUID() {
    return new JobControl().id(UUID.randomUUID().toString());
  }

  private static String getUserFacingId(String email) {
    String username =
        email
            .substring(0, email.indexOf('@'))
            .replaceAll("\\.", "-")
            .replaceAll("[^a-zA-Z0-9\\-]", "");
    String randomSuffix =
        UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "").substring(0, 4);
    return "user-pod-" + username + "-" + randomSuffix;
  }
}
