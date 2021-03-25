package org.pmiops.workbench.workspaces;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO eric: go through each method, provide dependencies and swap out requiements

public class WorkspaceAuthService {
//
//  @Override
//  public FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(
//      WorkspaceAccessLevel updatedAccess, FirecloudWorkspaceACLUpdate currentUpdate) {
//    if (updatedAccess == WorkspaceAccessLevel.OWNER) {
//      currentUpdate.setCanShare(true);
//      currentUpdate.setCanCompute(true);
//      currentUpdate.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
//    } else if (updatedAccess == WorkspaceAccessLevel.WRITER) {
//      currentUpdate.setCanShare(false);
//      currentUpdate.setCanCompute(true);
//      currentUpdate.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
//    } else if (updatedAccess == WorkspaceAccessLevel.READER) {
//      currentUpdate.setCanShare(false);
//      currentUpdate.setCanCompute(false);
//      currentUpdate.setAccessLevel(WorkspaceAccessLevel.READER.toString());
//    } else {
//      currentUpdate.setCanShare(false);
//      currentUpdate.setCanCompute(false);
//      currentUpdate.setAccessLevel(WorkspaceAccessLevel.NO_ACCESS.toString());
//    }
//    return currentUpdate;
//  }
//
//  public Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
//      String workspaceNamespace, String firecloudName) {
//    FirecloudWorkspaceACL aclResp =
//        fireCloudService.getWorkspaceAclAsService(workspaceNamespace, firecloudName);
//
//    // Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
//    // instead. Run this through a typed Gson conversion process to parse into the desired type.
//    Type accessEntryType = new TypeToken<Map<String, FirecloudWorkspaceAccessEntry>>() {}.getType();
//    Gson gson = new Gson();
//    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
//  }
//
//  public DbWorkspace updateWorkspaceAcls(
//      DbWorkspace workspace,
//      Map<String, WorkspaceAccessLevel> updatedAclsMap,
//      String registeredUsersGroup) {
//    // userRoleMap is a map of the new permissions for ALL users on the ws
//    Map<String, FirecloudWorkspaceAccessEntry> aclsMap =
//        getFirecloudWorkspaceAcls(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
//
//    // Iterate through existing roles, update/remove them
//    ArrayList<FirecloudWorkspaceACLUpdate> updateACLRequestList = new ArrayList<>();
//    Map<String, WorkspaceAccessLevel> toAdd = new HashMap<>(updatedAclsMap);
//    for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : aclsMap.entrySet()) {
//      String currentUserEmail = entry.getKey();
//      WorkspaceAccessLevel updatedAccess = toAdd.get(currentUserEmail);
//      if (updatedAccess != null) {
//        FirecloudWorkspaceACLUpdate currentUpdate = new FirecloudWorkspaceACLUpdate();
//        currentUpdate.setEmail(currentUserEmail);
//        currentUpdate = updateFirecloudAclsOnUser(updatedAccess, currentUpdate);
//        updateACLRequestList.add(currentUpdate);
//        toAdd.remove(currentUserEmail);
//      } else {
//        // This is how to remove a user from the FireCloud ACL:
//        // Pass along an update request with NO ACCESS as the given access level.
//        // Note: do not do groups.  Unpublish will pass the specific NO_ACCESS acl
//        // TODO [jacmrob] : have all users pass NO_ACCESS explicitly? Handle filtering on frontend?
//        if (!currentUserEmail.equals(registeredUsersGroup)) {
//          FirecloudWorkspaceACLUpdate removedUser = new FirecloudWorkspaceACLUpdate();
//          removedUser.setEmail(currentUserEmail);
//          removedUser = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, removedUser);
//          updateACLRequestList.add(removedUser);
//        }
//      }
//    }
//
//    // Iterate through remaining new roles; add them
//    for (Map.Entry<String, WorkspaceAccessLevel> remainingRole : toAdd.entrySet()) {
//      FirecloudWorkspaceACLUpdate newUser = new FirecloudWorkspaceACLUpdate();
//      newUser.setEmail(remainingRole.getKey());
//      newUser = updateFirecloudAclsOnUser(remainingRole.getValue(), newUser);
//      updateACLRequestList.add(newUser);
//    }
//    FirecloudWorkspaceACLUpdateResponseList fireCloudResponse =
//        fireCloudService.updateWorkspaceACL(
//            workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), updateACLRequestList);
//    if (fireCloudResponse.getUsersNotFound().size() != 0) {
//      String usersNotFound = "";
//      for (int i = 0; i < fireCloudResponse.getUsersNotFound().size(); i++) {
//        if (i > 0) {
//          usersNotFound += ", ";
//        }
//        usersNotFound += fireCloudResponse.getUsersNotFound().get(i).getEmail();
//      }
//      throw new BadRequestException(usersNotFound);
//    }
//
//    // Finally, keep OWNER and billing project users in lock-step. In Rawls, OWNER does not grant
//    // canCompute on the workspace / billing project, nor does it grant the ability to grant
//    // canCompute to other users. See RW-3009 for details.
//    for (String email : Sets.union(updatedAclsMap.keySet(), aclsMap.keySet())) {
//      String fromAccess =
//          aclsMap
//              .getOrDefault(email, new FirecloudWorkspaceAccessEntry().accessLevel(""))
//              .getAccessLevel();
//      WorkspaceAccessLevel toAccess =
//          updatedAclsMap.getOrDefault(email, WorkspaceAccessLevel.NO_ACCESS);
//      if (FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER != toAccess) {
//        log.info(
//            String.format(
//                "removing user '%s' from billing project '%s'",
//                email, workspace.getWorkspaceNamespace()));
//        fireCloudService.removeOwnerFromBillingProject(
//            email, workspace.getWorkspaceNamespace(), Optional.empty());
//      } else if (!FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER == toAccess) {
//        log.info(
//            String.format(
//                "adding user '%s' to billing project '%s'",
//                email, workspace.getWorkspaceNamespace()));
//        fireCloudService.addOwnerToBillingProject(email, workspace.getWorkspaceNamespace());
//      }
//    }
//
//    return this.saveWithLastModified(workspace);
//  }
//
//
//  @Override
//  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId)
//      throws IllegalArgumentException {
//    String userAccess =
//        fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getAccessLevel();
//    if (PROJECT_OWNER_ACCESS_LEVEL.equals(userAccess)) {
//      return WorkspaceAccessLevel.OWNER;
//    }
//    return Optional.ofNullable(WorkspaceAccessLevel.fromValue(userAccess))
//        .orElseThrow(
//            () -> new IllegalArgumentException("Unrecognized access level: " + userAccess));
//  }
//
//  @Override
//  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(
//      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
//    final WorkspaceAccessLevel access;
//    try {
//      access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
//    } catch (IllegalArgumentException e) {
//      throw new ServerErrorException(e);
//    }
//    if (requiredAccess.compareTo(access) > 0) {
//      throw new ForbiddenException(
//          String.format(
//              "You do not have sufficient permissions to access workspace %s/%s",
//              workspaceNamespace, workspaceId));
//    } else {
//      return access;
//    }
//  }
//
//  @Override
//  public DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
//      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel) {
//    enforceWorkspaceAccessLevel(
//        workspaceNamespace, workspaceId, workspaceAccessLevel);
//    DbWorkspace workspace = getRequired(workspaceNamespace, workspaceId);
//    // Because we've already checked that the user has access to the workspace in question,
//    // we don't need to check their membership in the authorization domain for the CDR version
//    // associated with the workspace.
//    CdrVersionContext.setCdrVersionNoCheckAuthDomain(workspace.getCdrVersion());
//    return workspace;
//  }
}
