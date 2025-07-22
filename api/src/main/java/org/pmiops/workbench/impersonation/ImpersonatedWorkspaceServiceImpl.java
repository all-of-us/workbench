package org.pmiops.workbench.impersonation;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
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
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FireCloudService firecloudService;
  private final FirecloudMapper firecloudMapper;
  private final ImpersonatedFirecloudService impersonatedFirecloudService;
  private final InitialCreditsService initialCreditsService;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public ImpersonatedWorkspaceServiceImpl(
      BillingProjectAuditor billingProjectAuditor,
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FireCloudService firecloudService,
      FirecloudMapper firecloudMapper,
      ImpersonatedFirecloudService impersonatedFirecloudService,
      InitialCreditsService initialCreditsService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.billingProjectAuditor = billingProjectAuditor;
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.firecloudMapper = firecloudMapper;
    this.firecloudService = firecloudService;
    this.impersonatedFirecloudService = impersonatedFirecloudService;
    this.initialCreditsService = initialCreditsService;
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
          .toApiWorkspaceResponseList(
              workspaceDao,
              impersonatedFirecloudService.getWorkspaces(dbUser),
              initialCreditsService)
          .stream()
          .filter(response -> response.getAccessLevel() == WorkspaceAccessLevel.OWNER)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  @Override
  public List<RawlsWorkspaceListResponse> getOwnedWorkspacesOrphanedInRawls(String username) {
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

  private boolean isOwner(RawlsWorkspaceListResponse response) {
    return firecloudMapper.fcToApiWorkspaceAccessLevel(response.getAccessLevel())
        == WorkspaceAccessLevel.OWNER;
  }

  private List<RawlsWorkspaceListResponse> notInAouDb(
      List<RawlsWorkspaceListResponse> rawlsWorkspaces) {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    var rawlsWorkspacesByUuid =
        rawlsWorkspaces.stream()
            .collect(
                Collectors.toMap(
                    fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                    fcWorkspace -> fcWorkspace));

    Set<String> rawlsUuids = rawlsWorkspacesByUuid.keySet();

    Set<String> dbWorkspaceUuids =
        workspaceDao.findActiveByFirecloudUuidIn(rawlsUuids).stream()
            .map(DbWorkspace::getFirecloudUuid)
            .collect(Collectors.toSet());

    // return FC workspaces which are not in the AoU DB
    return Sets.difference(rawlsUuids, dbWorkspaceUuids).stream()
        .map(rawlsWorkspacesByUuid::get)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteWorkspace(
      String username,
      String workspaceNamespace,
      String workspaceTerraName,
      boolean deleteBillingProjects) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    // also confirms that the workspace exists in the DB
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);

    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    try {
      impersonatedFirecloudService.deleteSamKubernetesResourcesInWorkspace(
          dbUser, dbWorkspace.getGoogleProject());
    } catch (Exception e) {
      // Ignore exceptions and proceed with workspace deletion.
      // We don't want an error here to stop us from trying to delete the workspace.
      logger.log(
          Level.WARNING,
          String.format(
              "An error occurred while deleting k8s resources for workspace %s",
              workspaceNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, workspaceNamespace, workspaceTerraName);
    } catch (Exception e) {
      throw new ServerErrorException(e);
    }

    workspaceDao.saveWithLastModified(
        dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED), dbUser);
    featuredWorkspaceDao.deleteDbFeaturedWorkspaceByWorkspace(dbWorkspace);

    if (deleteBillingProjects) {
      try {
        // use the real FirecloudService here because impersonation is not needed;
        // billing projects are owned by the App SA
        firecloudService.deleteBillingProject(workspaceNamespace);
        billingProjectAuditor.fireDeleteAction(workspaceNamespace);
      } catch (Exception e) {
        String msg =
            String.format(
                "Error deleting billing project %s: %s", workspaceNamespace, e.getMessage());
        logger.warning(msg);
      }
    }
  }

  @Override
  public void deleteOrphanedRawlsWorkspace(
      String username,
      String workspaceNamespace,
      String googleProject,
      String workspaceTerraName,
      boolean deleteBillingProjects) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    try {
      impersonatedFirecloudService.deleteSamKubernetesResourcesInWorkspace(dbUser, googleProject);
    } catch (Exception e) {
      // Ignore exceptions and proceed with workspace deletion.
      // We don't want an error here to stop us from trying to delete the workspace.
      logger.log(
          Level.WARNING,
          String.format(
              "An error occurred while deleting k8s resources for workspace %s",
              workspaceNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, workspaceNamespace, workspaceTerraName);
    } catch (Exception e) {
      throw new ServerErrorException(e);
    }

    if (deleteBillingProjects) {
      try {
        // use the real FirecloudService here because impersonation is not needed;
        // billing projects are owned by the App SA
        firecloudService.deleteBillingProject(workspaceNamespace);
      } catch (Exception e) {
        String msg =
            String.format(
                "Error deleting billing project %s: %s", workspaceNamespace, e.getMessage());
        logger.warning(msg);
      }
    }
  }

  @Override
  public void cleanupWorkspace(String workspaceNamespace, String lastModifiedBy) {
    DbWorkspace dbWorkspace = workspaceDao.getByNamespace(workspaceNamespace).orElse(null);
    if (dbWorkspace != null
        && dbWorkspace.getWorkspaceActiveStatusEnum() != WorkspaceActiveStatus.DELETED) {
      dbWorkspace.setLastModifiedBy(lastModifiedBy);
      dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
      workspaceDao.save(dbWorkspace);
    }
  }
}
