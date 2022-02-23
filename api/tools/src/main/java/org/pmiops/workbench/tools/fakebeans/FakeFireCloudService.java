package org.pmiops.workbench.tools.fakebeans;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;

public class FakeFireCloudService {
  public static FireCloudService fake() {
    return new FireCloudService() {
      @Override
      public String getApiBasePath() {
        return null;
      }

      @Override
      public boolean getFirecloudStatus() {
        return false;
      }

      @Override
      public FirecloudMe getMe() {
        return null;
      }

      @Override
      public void registerUser(String firstName, String lastName) {}

      @Override
      public String createAllOfUsBillingProject(
          String billingProjectName, String servicePerimeter) {
        return null;
      }

      @Override
      public void deleteBillingProject(String billingProjectName) {}

      @Override
      public FirecloudBillingProjectStatus getBillingProjectStatus(String billingProjectName) {
        return null;
      }

      @Override
      public void addOwnerToBillingProject(String ownerEmail, String billingProjectName) {}

      @Override
      public void removeOwnerFromBillingProject(
          String ownerEmailToRemove, String projectName, Optional<String> callerAccessToken) {}

      @Override
      public FirecloudWorkspaceDetails createWorkspace(
          String workspaceNamespace, String workspaceName, String authDomainName) {
        return null;
      }

      @Override
      public FirecloudWorkspaceDetails cloneWorkspace(
          String fromWorkspaceNamespace,
          String fromFirecloudName,
          String toWorkspaceNamespace,
          String toFirecloudName,
          String authDomainName) {
        return null;
      }

      @Override
      public FirecloudWorkspaceACL getWorkspaceAclAsService(
          String workspaceNamespace, String firecloudName) {
        return null;
      }

      @Override
      public FirecloudWorkspaceACLUpdateResponseList updateWorkspaceACL(
          String workspaceNamespace,
          String firecloudName,
          List<FirecloudWorkspaceACLUpdate> aclUpdates) {
        return null;
      }

      @Override
      public FirecloudWorkspaceResponse getWorkspaceAsService(
          String workspaceNamespace, String firecloudName) {
        return null;
      }

      @Override
      public FirecloudWorkspaceResponse getWorkspace(
          String workspaceNamespace, String firecloudName) {
        return null;
      }

      @Override
      public Optional<FirecloudWorkspaceResponse> getWorkspace(DbWorkspace dbWorkspace) {
        return Optional.empty();
      }

      @Override
      public List<FirecloudWorkspaceResponse> getWorkspaces() {
        return null;
      }

      @Override
      public void deleteWorkspace(String workspaceNamespace, String firecloudName) {}

      @Override
      public FirecloudManagedGroupWithMembers getGroup(String groupName) {
        return null;
      }

      @Override
      public FirecloudManagedGroupWithMembers createGroup(String groupName) {
        return null;
      }

      @Override
      public void addUserToGroup(String email, String groupName) {}

      @Override
      public void removeUserFromGroup(String email, String groupName) {}

      @Override
      public boolean isUserMemberOfGroupWithCache(String email, String groupName) {
        return false;
      }

      @Override
      public String staticNotebooksConvert(byte[] notebook) {
        return null;
      }

      @Override
      public void updateBillingAccount(String billingProjectName, String billingAccount) {}

      @Override
      public void updateBillingAccountAsService(String billingProjectName, String billingAccount) {}

      @Override
      public FirecloudNihStatus getNihStatus() {
        return null;
      }

      @Override
      public String createBillingProjectName() {
        return null;
      }

      @Override
      public boolean workspaceFileTransferComplete(
          String workspaceNamespace, String fireCloudName) {
        return false;
      }
    };
  }
}
