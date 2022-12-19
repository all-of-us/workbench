package org.pmiops.workbench.impersonation;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.workspaces.WorkspaceServiceImpl}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
@Service
public class ImpersonatedWorkspaceServiceImpl implements ImpersonatedWorkspaceService {
  private static final Logger LOGGER =
      Logger.getLogger(ImpersonatedWorkspaceServiceImpl.class.getName());

  private final BillingProjectAuditor billingProjectAuditor;
  private final FireCloudService firecloudService;
  private final ImpersonatedFirecloudService impersonatedFirecloudService;
  private final Provider<DbUser> userProvider;
  private final UserDao userDao;
  private final WorkspaceAuditor workspaceAuditor;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public ImpersonatedWorkspaceServiceImpl(
      BillingProjectAuditor billingProjectAuditor,
      FireCloudService firecloudService,
      ImpersonatedFirecloudService impersonatedFirecloudService,
      Provider<DbUser> userProvider,
      UserDao userDao,
      WorkspaceAuditor workspaceAuditor,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.billingProjectAuditor = billingProjectAuditor;
    this.firecloudService = firecloudService;
    this.impersonatedFirecloudService = impersonatedFirecloudService;
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.workspaceAuditor = workspaceAuditor;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResponse> getOwnedWorkspaces(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    try {
      return workspaceMapper
          .toApiWorkspaceResponses(workspaceDao, impersonatedFirecloudService.getWorkspaces(dbUser))
          .stream()
          .filter(response -> response.getAccessLevel() == WorkspaceAccessLevel.OWNER)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  @Override
  public void deleteWorkspace(String username, Workspace workspace) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    final String wsNamespace = workspace.getNamespace();
    final String wsId = workspace.getId();

    // also confirms that the workspace exists in the DB
    DbWorkspace dbWorkspace = workspaceDao.getRequired(wsNamespace, wsId);

    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, wsId);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }

    dbWorkspace =
        workspaceDao.saveWithLastModified(
            dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED),
            userProvider.get());
    workspaceAuditor.fireDeleteAction(dbWorkspace);

    try {
      // use the real FirecloudService here because impersonation is not needed;
      // billing projects are owned by the App SA
      firecloudService.deleteBillingProject(wsNamespace);
      billingProjectAuditor.fireDeleteAction(wsNamespace);
    } catch (Exception e) {
      String msg =
          String.format("Error deleting billing project %s: %s", wsNamespace, e.getMessage());
      LOGGER.warning(msg);
    }
  }
}
