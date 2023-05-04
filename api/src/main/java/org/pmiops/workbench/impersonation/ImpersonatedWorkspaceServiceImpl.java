package org.pmiops.workbench.impersonation;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
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
  private static final Logger logger =
      Logger.getLogger(ImpersonatedWorkspaceServiceImpl.class.getName());

  private final BillingProjectAuditor billingProjectAuditor;
  private final FirecloudMapper firecloudMapper;
  private final FireCloudService firecloudService;
  private final ImpersonatedFirecloudService impersonatedFirecloudService;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public ImpersonatedWorkspaceServiceImpl(
      BillingProjectAuditor billingProjectAuditor,
      FirecloudMapper firecloudMapper,
      FireCloudService firecloudService,
      ImpersonatedFirecloudService impersonatedFirecloudService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.billingProjectAuditor = billingProjectAuditor;
    this.firecloudMapper = firecloudMapper;
    this.firecloudService = firecloudService;
    this.impersonatedFirecloudService = impersonatedFirecloudService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResponse> getOwnedWorkspaces(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      logger.warning(String.format("user %s not found", username));
      return Collections.emptyList();
    }

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
  public List<FirecloudWorkspaceResponse> getOwnedWorkspacesOrphanedInRawls(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      logger.warning(String.format("user %s not found", username));
      return Collections.emptyList();
    }

    try {
      // return all Rawls workspaces which are NOT in the AoU DB (or deleted)
      // see WorkspaceMapper.toApiWorkspaceResponses() for the more typical case (active in AoU DB)
      return notInAouDb(impersonatedFirecloudService.getWorkspaces(dbUser)).stream()
          .filter(this::isOwner)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  private boolean isOwner(FirecloudWorkspaceResponse response) {
    return firecloudMapper.fcToApiWorkspaceAccessLevel(response.getAccessLevel())
        == WorkspaceAccessLevel.OWNER;
  }

  private List<FirecloudWorkspaceResponse> notInAouDb(
      List<FirecloudWorkspaceResponse> fcWorkspaces) {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    Map<String, FirecloudWorkspaceResponse> fcWorkspacesByUuid =
        fcWorkspaces.stream()
            .collect(
                Collectors.toMap(
                    fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                    fcWorkspace -> fcWorkspace));

    Set<String> fcUuids = fcWorkspacesByUuid.keySet();

    Set<String> dbWorkspaceUuids =
        workspaceDao.findActiveByFirecloudUuidIn(fcUuids).stream()
            .map(DbWorkspace::getFirecloudUuid)
            .collect(Collectors.toSet());

    // return FC workspaces which are not in the AoU DB
    return Sets.difference(fcUuids, dbWorkspaceUuids).stream()
        .map(fcWorkspacesByUuid::get)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteWorkspace(String username, String wsNamespace, String wsId) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    // also confirms that the workspace exists in the DB
    DbWorkspace dbWorkspace = workspaceDao.getRequired(wsNamespace, wsId);

    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    try {
      impersonatedFirecloudService.deleteSamKubernetesResourcesInWorkspace(
          dbUser, dbWorkspace.getGoogleProject());
    } catch (IOException e) {
      // Ignore exceptions and proceed with workspace deletion.
      // We don't want an error here to stop us from trying to delete the workspace.
      logger.log(
          Level.WARNING,
          String.format(
              "An error occurred while deleting k8s resources for workspace %s", wsNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, wsId);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }

    workspaceDao.saveWithLastModified(
        dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED), dbUser);

    try {
      // use the real FirecloudService here because impersonation is not needed;
      // billing projects are owned by the App SA
      firecloudService.deleteBillingProject(wsNamespace);
      billingProjectAuditor.fireDeleteAction(wsNamespace);
    } catch (Exception e) {
      String msg =
          String.format("Error deleting billing project %s: %s", wsNamespace, e.getMessage());
      logger.warning(msg);
    }
  }

  @Override
  public void deleteOrphanedRawlsWorkspace(
      String username, String wsNamespace, String googleProject, String wsId) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    try {
      impersonatedFirecloudService.deleteSamKubernetesResourcesInWorkspace(dbUser, googleProject);
    } catch (IOException e) {
      // Ignore exceptions and proceed with workspace deletion.
      // We don't want an error here to stop us from trying to delete the workspace.
      logger.log(
          Level.WARNING,
          String.format(
              "An error occurred while deleting k8s resources for workspace %s", wsNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, wsId);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }

    // TODO
    // I can't figure out how to make this work from a tool

    // 2023-05-03 18:52:06.815  WARN 20371 --- [           main]
    // o.p.w.i.ImpersonatedWorkspaceServiceImpl :
    // Error deleting billing project aou-rw-test-cce862eb: No qualifying bean of type
    // 'org.pmiops.workbench.rawls.api.BillingV2Api' available: expected at least 1 bean which
    // qualifies as autowire candidate. Dependency annotations:
    // {@org.springframework.beans.factory.annotation.Qualifier(value="serviceAccountBillingV2Api")}

    //    try {
    //      // use the real FirecloudService here because impersonation is not needed;
    //      // billing projects are owned by the App SA
    //      firecloudService.deleteBillingProject(wsNamespace);
    //    } catch (Exception e) {
    //      String msg =
    //          String.format("Error deleting billing project %s: %s", wsNamespace, e.getMessage());
    //      logger.warning(msg);
    //    }
  }
}
