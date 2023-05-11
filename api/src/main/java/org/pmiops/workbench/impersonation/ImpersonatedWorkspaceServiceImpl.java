package org.pmiops.workbench.impersonation;

import static org.pmiops.workbench.impersonation.ImpersonatedFirecloudServiceImpl.SAM_BILLING_PROJECT_NAME;
import static org.pmiops.workbench.impersonation.ImpersonatedFirecloudServiceImpl.SAM_PROJECT_OWNER_NAME;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.dsde.workbench.client.sam.model.UserResourcesResponse;
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
  public Map<String, String> getOwnedWorkspacesOrphanedInSam(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      logger.warning(String.format("user %s not found", username));
      return Collections.emptyMap();
    }

    try {
      Set<String> rawlsNamespaces =
          impersonatedFirecloudService.getWorkspaces(dbUser).stream()
              .map(RawlsWorkspaceListResponse::getWorkspace)
              .map(RawlsWorkspaceDetails::getNamespace)
              .collect(Collectors.toSet());
      var ownedSamResources =
          impersonatedFirecloudService.getSamWorkspaceResources(dbUser).stream()
              .filter(this::isProjectOwner)
              .map(UserResourcesResponse::getResourceId)
              .collect(Collectors.toList());
      var samBillingProjectMap =
          ownedSamResources.stream()
              .limit(200)
              .flatMap(r -> maybeGetBillingProjectMapEntry(dbUser, r))
              .collect(Collectors.toList());
      var samNotInRawlsMap =
          samBillingProjectMap.stream()
              .filter(e -> !rawlsNamespaces.contains(e.getKey()))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      logger.info(
          String.format(
              "Found %d rawls namespaces and %d sam resources, "
                  + "of which %d were resolved to billing projects and %d were not present in Rawls",
              rawlsNamespaces.size(),
              ownedSamResources.size(),
              samBillingProjectMap.size(),
              samNotInRawlsMap.size()));
      return samNotInRawlsMap;
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  private boolean isProjectOwner(UserResourcesResponse response) {
    return Optional.ofNullable(response.getDirect())
        .map(r -> r.getRoles().contains(SAM_PROJECT_OWNER_NAME))
        .orElse(false);
  }

  // attempt to resolve a Billing Project for a Workspace ID:
  // success -> return a streamed Map.Entry of <Billing Project, Workspace Resource ID>
  // failure -> Stream.empty()
  private Stream<Entry<String, String>> maybeGetBillingProjectMapEntry(
      DbUser dbUser, String workspaceResourceId) {
    try {
      return impersonatedFirecloudService
          .getSamWorkspacePolicies(dbUser, workspaceResourceId)
          .stream()
          // filter to the project-owner policy (policies?)
          .filter(e -> e.getPolicyName().equals(SAM_PROJECT_OWNER_NAME))
          // map to member policies, if any exist
          .flatMap(e -> Stream.ofNullable(e.getPolicy().getMemberPolicies()))
          .flatMap(Collection::stream)
          // filter to the M.P. corresponding to the billing project resource type
          .filter(pi -> pi.getResourceTypeName().equals(SAM_BILLING_PROJECT_NAME))
          .map(pi -> Map.entry(pi.getResourceId(), workspaceResourceId));
    } catch (IOException e) {
      logger.severe(
          String.format(
              "Exception retrieving policies for %s: %s", workspaceResourceId, e.getMessage()));
      return Stream.empty();
    }
  }

  @Override
  public void deleteWorkspace(
      String username, String wsNamespace, String wsId, boolean deleteBillingProjects) {
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
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, wsId);
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
      String wsId,
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
      impersonatedFirecloudService.deleteWorkspace(dbUser, wsNamespace, wsId);
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
}
