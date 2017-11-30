package org.pmiops.workbench.firecloud;

import java.util.List;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface FireCloudService {

  /**
   * @return true if the user making the current request is enabled in FireCloud, false otherwise.
   */
  boolean isRequesterEnabledInFirecloud() throws ApiException;

  /**
   * Registers the user in Firecloud.
   * @param contactEmail an email address that can be used to contact this user
   * @param firstName the user's first name
   * @param lastName the user's last name
   */
  void registerUser(String contactEmail, String firstName, String lastName) throws ApiException;

  /**
   * Creates a billing project owned by AllOfUs.
   */
  void createAllOfUsBillingProject(String projectName) throws ApiException;

  /**
   * Adds the specified user to the specified billing project.
   */
  void addUserToBillingProject(String email, String projectName) throws ApiException;

  /**
   * Creates a new FC workspace.
   */
  void createWorkspace(String projectName, String workspaceName) throws ApiException;

  /**
   * Retrieves all billing project memberships for the user from FireCloud.
   */
  List<BillingProjectMembership> getBillingProjectMemberships() throws ApiException;

  WorkspaceACLUpdateResponseList updateWorkspaceACL(String projectName, String workspaceName, List<WorkspaceACLUpdate> aclUpdates) throws ApiException;

  WorkspaceResponse getWorkspace(String projectName, String workspaceName) throws ApiException;
}
