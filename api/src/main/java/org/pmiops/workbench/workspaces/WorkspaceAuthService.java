package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import jakarta.inject.Provider;
import java.util.List;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuthService {
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceDao workspaceDao;
  private final AccessTierService accessTierService;
  private final InitialCreditsService initialCreditsService;

  @Autowired
  public WorkspaceAuthService(
      AccessTierService accessTierService,
      InitialCreditsService initialCreditsService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceDao workspaceDao) {
    this.accessTierService = accessTierService;
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
