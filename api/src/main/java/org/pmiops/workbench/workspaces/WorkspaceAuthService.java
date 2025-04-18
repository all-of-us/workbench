package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import jakarta.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.access.AccessTierService;
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
import org.pmiops.workbench.initialcredits.InitialCreditsService;
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
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceDao workspaceDao;
  private final AccessTierService accessTierService;
  private final InitialCreditsService initialCreditsService;

  @Autowired
  public WorkspaceAuthService(
      AccessTierService accessTierService,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceDao workspaceDao) {
    this.accessTierService = accessTierService;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
  }

  /*
   * This function will check if a workspace is eligible to be using initial credits.
   * This involves checking whether it's using the initial credits billing account
   * and that their initial credits have not been exhausted or expired.
   */
  public void validateInitialCreditUsage(String workspaceNamespace, String workspaceTerraName)
      throws ForbiddenException {
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);
    DbUser creator = workspace.getCreator();
    if (isInitialCredits(workspace.getBillingAccountName(), workbenchConfigProvider.get())
        && (workspace.isInitialCreditsExhausted()
            || initialCreditsService.areUserCreditsExpired(creator))) {
      throw new ForbiddenException(
          String.format(
              "Workspace (%s) is using initial credits that have either expired or have been exhausted.",
              workspaceNamespace));
    }
  }

  public WorkspaceAccessLevel getWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceTerraName) throws IllegalArgumentException {
    String userAccess =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceTerraName)
            .getAccessLevel()
            .toString();
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
      String workspaceNamespace, String workspaceTerraName, WorkspaceAccessLevel requiredAccess) {
    final WorkspaceAccessLevel access;
    try {
      access = getWorkspaceAccessLevel(workspaceNamespace, workspaceTerraName);
    } catch (IllegalArgumentException e) {
      throw new ServerErrorException(e);
    }
    if (requiredAccess.compareTo(access) > 0) {
      throw new ForbiddenException(
          String.format(
              "You do not have sufficient permissions to access workspace %s/%s",
              workspaceNamespace, workspaceTerraName));
    } else {
      return access;
    }
  }

  public DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace,
      String workspaceTerraName,
      WorkspaceAccessLevel workspaceAccessLevel) {
    enforceWorkspaceAccessLevel(workspaceNamespace, workspaceTerraName, workspaceAccessLevel);
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);
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

  /**
   * Throw ForbiddenException if logged in user doesn't have the same Tier Access as that of
   * workspace
   *
   * @param dbWorkspace
   */
  public void validateWorkspaceTierAccess(DbWorkspace dbWorkspace) {
    String workspaceAccessTier = dbWorkspace.getCdrVersion().getAccessTier().getShortName();

    List<String> accessTiers = accessTierService.getAccessTierShortNamesForUser(userProvider.get());

    if (!accessTiers.contains(workspaceAccessTier)) {
      throw new ForbiddenException(
          String.format(
              "User with username %s does not have access to the '%s' access tier required by "
                  + "workspace '%s'",
              userProvider.get().getUsername(), workspaceAccessTier, dbWorkspace.getName()));
    }
  }
}
