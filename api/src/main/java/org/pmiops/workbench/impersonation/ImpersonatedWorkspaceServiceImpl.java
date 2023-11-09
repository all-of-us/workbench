package org.pmiops.workbench.impersonation;

import static org.pmiops.workbench.impersonation.ImpersonatedFirecloudServiceImpl.SAM_RESOURCE_OWNER_NAME;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.UserResourcesResponse;
import org.javers.common.collections.Lists;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
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

  private boolean isRawlsOwner(RawlsWorkspaceListResponse response) {
    return firecloudMapper.fcToApiWorkspaceAccessLevel(response.getAccessLevel())
        == WorkspaceAccessLevel.OWNER;
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
          .filter(this::isRawlsOwner)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
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

  private boolean isSamOwner(UserResourcesResponse response) {
    return Optional.ofNullable(response.getDirect())
        .map(r -> r.getRoles().contains(SAM_RESOURCE_OWNER_NAME))
        .orElse(false);
  }

  private boolean hasNoChildren(DbUser dbUser, String workspaceResourceId) {
    try {
      return impersonatedFirecloudService
          .getSamWorkspaceResourceChildren(dbUser, workspaceResourceId)
          .isEmpty();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<String> getOwnedWorkspacesOrphanedInSam(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      logger.warning(String.format("user %s not found", username));
      return Collections.emptyList();
    }

    try {
      var rawlsIds =
          impersonatedFirecloudService.getWorkspaces(dbUser).stream()
              .filter(this::isRawlsOwner)
              .map(RawlsWorkspaceListResponse::getWorkspace)
              .map(RawlsWorkspaceDetails::getWorkspaceId)
              .collect(Collectors.toList());
      var samResources =
          impersonatedFirecloudService.getSamWorkspaceResources(dbUser).stream()
              .filter(this::isSamOwner)
              .map(UserResourcesResponse::getResourceId)
              .limit(100) // TEMP to avoid spamming sam
              .filter(r -> hasNoChildren(dbUser, r))
              .collect(Collectors.toList());

      var samNotInRawls = Lists.difference(samResources, rawlsIds);

      logger.info(
          String.format(
              "Found %d owned Rawls workspace IDs and %d owned Sam resources, of which %d were not present in Rawls",
              rawlsIds.size(), samResources.size(), samNotInRawls.size()));
      return samNotInRawls;
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  @Override
  public void deleteWorkspace(
      String username, String wsNamespace, String firecloudName, boolean deleteBillingProjects) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    // also confirms that the workspace exists in the DB
    DbWorkspace dbWorkspace = workspaceDao.getRequired(wsNamespace, firecloudName);

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
              "An error occurred while deleting k8s resources for workspace %s", wsNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, firecloudName);
    } catch (Exception e) {
      throw new ServerErrorException(e);
    }

    workspaceDao.saveWithLastModified(
        dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED), dbUser);

    if (deleteBillingProjects) {
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
  }

  @Override
  public void deleteOrphanedRawlsWorkspace(
      String username,
      String wsNamespace,
      String googleProject,
      String firecloudName,
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
              "An error occurred while deleting k8s resources for workspace %s", wsNamespace),
          e);
    }

    try {
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, firecloudName);
    } catch (Exception e) {
      throw new ServerErrorException(e);
    }

    if (deleteBillingProjects) {
      try {
        // use the real FirecloudService here because impersonation is not needed;
        // billing projects are owned by the App SA
        firecloudService.deleteBillingProject(wsNamespace);
      } catch (Exception e) {
        String msg =
            String.format("Error deleting billing project %s: %s", wsNamespace, e.getMessage());
        logger.warning(msg);
      }
    }
  }

  @Override
  public void deleteOrphanedSamWorkspace(String username, String wsUuid) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    try {
      impersonatedFirecloudService.deleteSamWorkspaceResource(dbUser, wsUuid);
    } catch (Exception e) {
      logger.severe(
          String.format(
              "Could not delete workspace %s: %s[%s]",
              wsUuid, e.getClass().getName(), e.getMessage()));
      throw new ServerErrorException(e);
    }
  }
}
