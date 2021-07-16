package org.pmiops.workbench.firecloud;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;

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

  /**
   * Registers the user in Firecloud.
   *
   * @param contactEmail an email address that can be used to contact this user
   * @param firstName the user's first name
   * @param lastName the user's last name
   */
  void registerUser(String contactEmail, String firstName, String lastName);

  /** Creates a billing project owned by AllOfUs. */
  void createAllOfUsBillingProject(String projectName, String servicePerimeter);

  void deleteBillingProject(String billingProject);

  /** Get Billing Project Status */
  FirecloudBillingProjectStatus getBillingProjectStatus(String projectName);

  /** Adds the specified user as an owner to the specified billing project. */
  void addOwnerToBillingProject(String ownerEmail, String projectName);

  /**
   * Removes the specified user as an owner from the specified billing project. Since FireCloud
   * users cannot remove themselves, we need to supply the credential of a different user which will
   * retain ownership to make the call.
   *
   * <p>The call is made by the SA by default. An optional callerAccessToken can be passed in to use
   * that as the caller instead.
   */
  void removeOwnerFromBillingProject(
      String ownerEmailToRemove, String projectName, Optional<String> callerAccessToken);

  /** Creates a new FC workspace. */
  FirecloudWorkspace createWorkspace(
      String projectName, String workspaceName, String authDomainName);

  FirecloudWorkspace cloneWorkspace(
      String fromProject, String fromName, String toProject, String toName, String authDomainName);

  /** Retrieves all billing project memberships for the user from FireCloud. */
  List<FirecloudBillingProjectMembership> getBillingProjectMemberships();

  FirecloudWorkspaceACL getWorkspaceAclAsService(String projectName, String workspaceName);

  FirecloudWorkspaceACLUpdateResponseList updateWorkspaceACL(
      String projectName, String workspaceName, List<FirecloudWorkspaceACLUpdate> aclUpdates);

  FirecloudWorkspaceResponse getWorkspaceAsService(String projectName, String workspaceName);

  /**
   * Requested field options specified here:
   * https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#heading=h.xgjl2srtytjt
   */
  FirecloudWorkspaceResponse getWorkspace(String projectName, String workspaceName);

  Optional<FirecloudWorkspaceResponse> getWorkspace(DbWorkspace dbWorkspace);

  List<FirecloudWorkspaceResponse> getWorkspaces();

  void deleteWorkspace(String projectName, String workspaceName);

  FirecloudManagedGroupWithMembers getGroup(String groupname);

  FirecloudManagedGroupWithMembers createGroup(String groupName);

  void addUserToGroup(String email, String groupName);

  void removeUserFromGroup(String email, String groupName);

  boolean isUserMemberOfGroupWithCache(String email, String groupName);

  String staticNotebooksConvert(byte[] notebook);

  String staticNotebooksConvertAsService(byte[] notebook);

  /** Update billing account using end user credential. */
  void updateBillingAccount(String billingProject, String billingAccount);
  /** Update billing account using APP's service account. */
  void updateBillingAccountAsService(String billingProject, String billingAccount);

  /**
   * Fetches the status of the currently-authenticated user's linkage to NIH's eRA Commons system.
   *
   * <p>Returns null if the FireCloud user is not found or if the user has no NIH linkage.
   */
  FirecloudNihStatus getNihStatus();

  ApiClient getApiClientWithImpersonation(String email) throws IOException;
}
