package org.pmiops.workbench.firecloud;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.RandomUtils;

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface for
 * internal use.
 */
public interface FireCloudService {

  String WORKSPACE_DELIMITER = "__";

  /** Returns the base path for the FireCloud API. Exposed for integration testing purposes only. */
  @VisibleForTesting
  String getApiBasePath();

  /** @return true if firecloud is okay, false if firecloud is down. */
  boolean getFirecloudStatus();

  /** @return the FireCloud profile for the requesting user. */
  FirecloudMe getMe();

  /** Registers the user in Firecloud. */
  void registerUser();

  /** Creates a billing project owned by AllOfUs. */
  String createAllOfUsBillingProject(String billingProjectName, String servicePerimeter);

  void deleteBillingProject(String billingProjectName);

  /** Adds the specified user as an owner to the specified billing project. */
  void addOwnerToBillingProject(String ownerEmail, String billingProjectName);

  /**
   * Removes the specified user as an owner from the specified billing project. Since FireCloud
   * users cannot remove themselves, we need to supply the credential of a different user which will
   * retain ownership to make the call.
   *
   * <p>The call is made by the SA by default. An optional callerAccessToken can be passed in to use
   * that as the caller instead.
   */
  void removeOwnerFromBillingProjectAsService(String ownerEmailToRemove, String projectName);

  int NUM_RANDOM_CHARS = 20;
  String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz";

  static String toFirecloudName(String workbenchName) {
    // Derive a firecloud-compatible name from the provided name.
    String strippedName = workbenchName.toLowerCase().replaceAll("[^0-9a-z]", "");
    // If the stripped name has no chars, generate a random name.
    if (strippedName.isEmpty()) {
      strippedName = RandomUtils.generateRandomChars(RANDOM_CHARS, NUM_RANDOM_CHARS);
    }
    return strippedName;
  }

  /** Creates a new FC workspace. */
  RawlsWorkspaceDetails createWorkspace(
      String workspaceNamespace, String workspaceName, String authDomainName);

  RawlsWorkspaceDetails cloneWorkspace(
      String fromWorkspaceNamespace,
      String fromFirecloudName,
      String toWorkspaceNamespace,
      String toFirecloudName,
      String authDomainName);

  RawlsWorkspaceACL getWorkspaceAclAsService(String workspaceNamespace, String firecloudName);

  /**
   * Make a Terra PATCH request with a list of ACL update requests for a specific workspace. Only
   * makes the changes specified. Choose the access level "NO ACCESS" to remove access.
   *
   * @param workspaceNamespace the Namespace (Terra Billing Project) of the Workspace to modify
   * @param firecloudName the Terra Name of the Workspace to modify
   * @param aclUpdates
   * @return
   */
  RawlsWorkspaceACLUpdateResponseList updateWorkspaceACL(
      String workspaceNamespace, String firecloudName, List<RawlsWorkspaceACLUpdate> aclUpdates);

  RawlsWorkspaceResponse getWorkspaceAsService(String workspaceNamespace, String firecloudName);

  /**
   * Requested field options specified here:
   * https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#heading=h.xgjl2srtytjt
   */
  RawlsWorkspaceResponse getWorkspace(String workspaceNamespace, String firecloudName);

  Optional<RawlsWorkspaceResponse> getWorkspace(DbWorkspace dbWorkspace);

  List<RawlsWorkspaceListResponse> getWorkspaces();

  void deleteWorkspace(String workspaceNamespace, String firecloudName);

  FirecloudManagedGroupWithMembers getGroup(String groupName);

  FirecloudManagedGroupWithMembers createGroup(String groupName);

  void addUserToGroup(String email, String groupName);

  void removeUserFromGroup(String email, String groupName);

  boolean isUserMemberOfGroupWithCache(String email, String groupName);

  String staticJupyterNotebooksConvert(byte[] notebook);

  String staticRstudioNotebooksConvert(byte[] notebook);

  /** Update billing account using end user credential. */
  void updateBillingAccount(String billingProjectName, String billingAccount);

  /** Update billing account using APP's service account. */
  void updateBillingAccountAsService(String billingProjectName, String billingAccount);

  /**
   * Fetches the status of the currently-authenticated user's linkage to NIH's eRA Commons system.
   *
   * <p>Returns null if the FireCloud user is not found or if the user has no NIH linkage.
   */
  FirecloudNihStatus getNihStatus();

  /** Creates a random Billing Project name. */
  String createBillingProjectName();

  boolean workspaceFileTransferComplete(String workspaceNamespace, String fireCloudName);

  void acceptTermsOfService();

  boolean getUserTermsOfServiceStatusDeprecated();

  /**
   * Is the current user currently compliant with Terra's Terms of Service?
   *
   * <p>This will be true of the user has accepted the latest version OR we are in the rolling
   * acceptance window
   *
   * @param dbUser the current user (only used to construct error message)
   * @return true if Terra allows system usage based on ToS status
   */
  boolean isUserCompliantWithTerraToS(@Nonnull DbUser dbUser);

  /**
   * Has the current user accepted the <b>latest</b> Terra Terms of Service? Note: this is a
   * stricter requirement than {@link #isUserCompliantWithTerraToS(DbUser)}
   *
   * @param dbUser the current user (only used to construct error message)
   * @return true if the user is compliant with the latest Terra ToS
   */
  boolean hasUserAcceptedLatestTerraToS(@Nonnull DbUser dbUser);
}
