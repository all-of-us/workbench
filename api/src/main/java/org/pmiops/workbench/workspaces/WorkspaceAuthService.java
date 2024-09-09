package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.workspaces.WorkspaceUtils.isFreeTier;

import jakarta.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuthService {

  private static final Logger log = Logger.getLogger(WorkspaceAuthService.class.getName());

  public static final String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";
  private static final String FC_OWNER_ROLE = "OWNER";

  private final FireCloudService fireCloudService;
  private final Provider<DbUser> userProvider;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public WorkspaceAuthService(
      FireCloudService fireCloudService, Provider<DbUser> userProvider, WorkspaceDao workspaceDao) {
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.workspaceDao = workspaceDao;
  }

  /*
   * This function will check the workspace's billing status and throw a ForbiddenException
   * if it is inactive.
   *
   * There is no hard and fast rule on what operations should require active billing but
   * the general idea is that we should prevent operations that can either incur a non trivial
   * amount of Google Cloud computation costs (starting a notebook runtime) or increase the
   * monthly cost of the workspace (ex. creating GCS objects).
   */
  public void validateActiveBilling(
      String workspaceNamespace, String workspaceId, WorkbenchConfig workbenchConfig)
      throws ForbiddenException {
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    if (isFreeTier(workspace.getBillingAccountName(), workbenchConfig)
        && (workspace.isInitialCreditsExhausted() || workspace.isInitialCreditsExpired())) {
      throw new ForbiddenException(
          String.format("Workspace (%s) is in an inactive billing state", workspaceNamespace));
    }
  }

  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId)
      throws IllegalArgumentException {
    String userAccess =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getAccessLevel().toString();
    if (PROJECT_OWNER_ACCESS_LEVEL.equals(userAccess)) {
      return WorkspaceAccessLevel.OWNER;
    }
    return Optional.ofNullable(WorkspaceAccessLevel.fromValue(userAccess))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("Unrecognized access level: %s", userAccess)));
  }

  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
    final WorkspaceAccessLevel access;
    try {
      access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
    } catch (IllegalArgumentException e) {
      throw new ServerErrorException(e);
    }
    if (requiredAccess.compareTo(access) > 0) {
      throw new ForbiddenException(
          String.format(
              "You do not have sufficient permissions to access workspace %s/%s",
              workspaceNamespace, workspaceId));
    } else {
      return access;
    }
  }

  public DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel) {
    enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, workspaceAccessLevel);
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    // Because we've already checked that the user has access to the workspace in question,
    // we don't need to check their membership in the authorization domain for the CDR version
    // associated with the workspace.
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(workspace.getCdrVersion());
    return workspace;
  }

  public Map<String, RawlsWorkspaceAccessEntry> getFirecloudWorkspaceAcl(
      String workspaceNamespace, String firecloudName) {
    return FirecloudTransforms.extractAclResponse(
        fireCloudService.getWorkspaceAclAsService(workspaceNamespace, firecloudName));
  }

  private void updateAcl(
      DbWorkspace workspace, List<RawlsWorkspaceACLUpdate> updateACLRequestList) {

    RawlsWorkspaceACLUpdateResponseList fireCloudResponse =
        fireCloudService.updateWorkspaceACL(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), updateACLRequestList);

    if (!fireCloudResponse.getUsersNotFound().isEmpty()) {
      throw new BadRequestException(
          "users not found: "
              + fireCloudResponse.getUsersNotFound().stream()
                  .map(RawlsWorkspaceACLUpdate::getEmail)
                  .collect(Collectors.joining(", ")));
    }
  }

  private void updateAcl(DbWorkspace workspace, Map<String, WorkspaceAccessLevel> updatedAclMap) {
    updateAcl(
        workspace,
        updatedAclMap.entrySet().stream()
            .map(e -> FirecloudTransforms.buildAclUpdate(e.getKey(), e.getValue()))
            .collect(Collectors.toList()));
  }

  private void synchronizeOwnerBillingProjects(
      String billingProjectName,
      Set<String> usersToSynchronize,
      Map<String, WorkspaceAccessLevel> updatedAclMap,
      Map<String, RawlsWorkspaceAccessEntry> existingAclMap) {

    for (String email : usersToSynchronize) {
      String fromAccess =
          existingAclMap
              .getOrDefault(email, new RawlsWorkspaceAccessEntry().accessLevel("NO ACCESS"))
              .getAccessLevel();
      WorkspaceAccessLevel toAccess =
          updatedAclMap.getOrDefault(email, WorkspaceAccessLevel.NO_ACCESS);

      if (FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER != toAccess) {
        log.info(
            String.format(
                "removing user '%s' from billing project '%s'", email, billingProjectName));
        fireCloudService.removeOwnerFromBillingProjectAsService(email, billingProjectName);
      } else if (!FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER == toAccess) {
        log.info(
            String.format("adding user '%s' to billing project '%s'", email, billingProjectName));
        fireCloudService.addOwnerToBillingProject(email, billingProjectName);
      }
    }
  }

  public DbWorkspace patchWorkspaceAcl(
      DbWorkspace workspace, Map<String, WorkspaceAccessLevel> updatedAclMap) {

    updateAcl(workspace, updatedAclMap);

    // Keep OWNER and billing project users in lock-step. In Rawls, OWNER does not grant
    // canCompute on the workspace / billing project, nor does it grant the ability to grant
    // canCompute to other users. See RW-3009 for details.
    synchronizeOwnerBillingProjects(
        workspace.getWorkspaceNamespace(),
        updatedAclMap.keySet(),
        updatedAclMap,
        getFirecloudWorkspaceAcl(workspace.getWorkspaceNamespace(), workspace.getFirecloudName()));

    return workspaceDao.saveWithLastModified(workspace, userProvider.get());
  }
}
