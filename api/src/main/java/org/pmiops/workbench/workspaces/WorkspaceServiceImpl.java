package org.pmiops.workbench.workspaces;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DbWorkspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * <p>This needs to implement an interface to support Transactional
 */
@Service
@Primary
public class WorkspaceServiceImpl implements WorkspaceService {

  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private final ActionAuditQueryService actionAuditQueryService;
  private final Clock clock;
  private final InitialCreditsService initialCreditsService;
  private final Provider<DbUser> userProvider;
  private final UserRecentWorkspaceDao userRecentWorkspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public WorkspaceServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      Clock clock,
      InitialCreditsService initialCreditsService,
      Provider<DbUser> userProvider,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.clock = clock;
    this.initialCreditsService = initialCreditsService;
    this.userProvider = userProvider;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResponse> listWorkspaces() {

    List<Long> creatorWorkspaceIds =
        workspaceDao.findAllWorkspaceIdsByCreator(userProvider.get()).stream().toList();
    List<ActionAuditQueryService.WorkspaceIdWithRoleImpl> allWorkspaceIdsWithRole =
        new ArrayList<>();
    creatorWorkspaceIds.forEach(
        workspaceId -> {
          allWorkspaceIdsWithRole.add(
              new ActionAuditQueryService.WorkspaceIdWithRoleImpl(workspaceId, "OWNER"));
        });
    // Get ids and roles for all workspaces that have been shared with the current user
    List<ActionAuditQueryService.WorkspaceIdWithRoleImpl> collaboratorWorkspaceIdsWithRole =
        actionAuditQueryService
            .getWorkspaceIdsAndRolesByCollaboratorId(userProvider.get().getUserId())
            .stream()
            .filter(
                workspaceIdWithRole ->
                    !creatorWorkspaceIds.contains(workspaceIdWithRole.workspaceId()))
            .toList();
    allWorkspaceIdsWithRole.addAll(collaboratorWorkspaceIdsWithRole);

    List<WorkspaceResponse> allWorkspaces = new ArrayList<>();
    allWorkspaceIdsWithRole.forEach(
        workspaceIdWithRole -> {
          System.out.print("allWorkspaceIdsWithRole.forEach");
          System.out.print("\n");
          System.out.print(workspaceIdWithRole);
          System.out.print("\n");
          if (workspaceIdWithRole.workspaceId() != null) {
            DbWorkspace workspace =
                workspaceDao.findByWorkspaceId(workspaceIdWithRole.workspaceId());
            if (workspace != null && workspace.getRecoveryState() != null) {
              WorkspaceResponse response = new WorkspaceResponse();
              response.setAccessLevel(WorkspaceAccessLevel.fromValue(workspaceIdWithRole.role()));
              response.setWorkspace(
                  workspaceMapper.toApiWorkspace(workspace, null, initialCreditsService));
              allWorkspaces.add(response);
            }
          }
        });
    return allWorkspaces;
  }

  @Transactional
  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceTerraName) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);
    workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace);

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();

    List<ActionAuditQueryService.UserIdWithRoleImpl> collaboratorWorkspaceIdsWithRole =
        actionAuditQueryService.getWorkspaceUsersById(dbWorkspace.getWorkspaceId());

    String accessLevel =
        collaboratorWorkspaceIdsWithRole.stream()
            .filter(userIdWithRole -> userIdWithRole.userId() == userProvider.get().getUserId())
            .map(ActionAuditQueryService.UserIdWithRoleImpl::role)
            .findFirst()
            .orElse("NO_ACCESS");

    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.valueOf(accessLevel));
    Workspace workspace = workspaceMapper.toApiWorkspace(dbWorkspace, null, initialCreditsService);
    workspaceResponse.setWorkspace(workspace);

    return workspaceResponse;
  }

  @Override
  @Transactional
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return updateRecentWorkspaces(
        workspace, userProvider.get().getUserId(), new Timestamp(clock.instant().toEpochMilli()));
  }

  private DbUserRecentWorkspace updateRecentWorkspaces(
      DbWorkspace workspace, long userId, Timestamp lastAccessDate) {
    Optional<DbUserRecentWorkspace> maybeRecentWorkspace =
        userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspace.getWorkspaceId(), userId);
    final DbUserRecentWorkspace matchingRecentWorkspace =
        maybeRecentWorkspace
            .map(
                recentWorkspace -> {
                  recentWorkspace.setLastAccessDate(lastAccessDate);
                  return recentWorkspace;
                })
            .orElseGet(
                () ->
                    new DbUserRecentWorkspace(workspace.getWorkspaceId(), userId, lastAccessDate));
    userRecentWorkspaceDao.save(matchingRecentWorkspace);
    handleWorkspaceLimit(userId);
    return matchingRecentWorkspace;
  }

  private void handleWorkspaceLimit(long userId) {
    List<DbUserRecentWorkspace> userRecentWorkspaces =
        userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId);

    ArrayList<Long> idsToDelete = new ArrayList<>();
    while (userRecentWorkspaces.size() > RECENT_WORKSPACE_COUNT) {
      idsToDelete.add(userRecentWorkspaces.get(userRecentWorkspaces.size() - 1).getWorkspaceId());
      userRecentWorkspaces.remove(userRecentWorkspaces.size() - 1);
    }
    userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(userId, idsToDelete);
  }

  @Override
  public List<DbWorkspace> lookupWorkspacesByNamespace(Collection<String> workspaceNamespaces) {
    return workspaceDao.getByWorkspaceNamespaceIn(workspaceNamespaces);
  }
}
