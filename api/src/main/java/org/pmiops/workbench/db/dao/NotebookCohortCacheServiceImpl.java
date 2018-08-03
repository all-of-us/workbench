package org.pmiops.workbench.db.dao;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.NotebookCohortCache;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.sql.Timestamp;

@Service
public class NotebookCohortCacheServiceImpl implements NotebookCohortCacheService {

  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  NotebookCohortCacheDao notebookCohortCacheDao;
  @Autowired
  WorkspaceUserRoleDao workspaceUserRoleDao;

  public NotebookCohortCacheServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public NotebookCohortCacheDao getDao() {
    return notebookCohortCacheDao;
  }

  public WorkspaceUserRoleDao getworkspaceUserRoleDao() {
    return workspaceUserRoleDao;
  }

  /**
   * Checks if notebook for given workspace and user is already in cache
   * if yes, update the lastmodified time only
   * If no, check the number of cache entries for given workspace and user
   * if it is above 10, delete the row with least lastAccessTime and add a new entry
   * @param workspace
   * @param user
   * @param notebookName
   * @param lastAccessDate
   */
  @Override
  public void updateNotebook(Workspace workspace, User user, String notebookName,
      Timestamp lastAccessDate) {
    WorkspaceUserRole workspaceUserRole =
        getworkspaceUserRoleDao().findWorkspaceUserRolesByWorkspaceAndUser(workspace, user);
    if (workspaceUserRole == null) {
      throw new BadRequestException(String.format(
          "No workspace %s was found for user %s ", workspace.getName(), user.getUserId()));
    }
    NotebookCohortCache cache = getDao().findByUserWorkspaceIdAndNotebookName
        (workspaceUserRole, notebookName);
    if (cache == null) {
      handleWorkspaceUserLimitInCache(workspaceUserRole);
      cache = new NotebookCohortCache();
      cache.setUserWorkspaceId(workspaceUserRole);
      cache.setCohortId(null);
      cache.setLastAccessTime(lastAccessDate);
      cache.setNotebookName(notebookName);
    }
    cache.setLastAccessTime(lastAccessDate);
    getDao().save(cache);
  }

  /**
   * Checks if cohort is already in cache for given workspace and user
   * if yes, update the lastmodified time only
   * If no, check the number of cache entries for given workspace and user
   * if it is above 10, delete the row with least lastAccessTime and add a new entry
   * @param workspace
   * @param user
   * @param cohortId
   * @param lastAccessDate
   */
  @Override
  public void updateCohort(Workspace workspace, User user, long cohortId,
      Timestamp lastAccessDate) {
    WorkspaceUserRole workspaceUserRole =
        getworkspaceUserRoleDao().findWorkspaceUserRolesByWorkspaceAndUser(workspace, user);
    if (workspaceUserRole == null) {
      throw new BadRequestException(String.format(
          "No workspace %s was found for user %s ", workspace.getName(), user.getUserId()));
    }
    NotebookCohortCache cache = getDao().findByUserWorkspaceIdAndCohortId(workspaceUserRole, cohortId);
    if (cache == null) {
      handleWorkspaceUserLimitInCache(workspaceUserRole);
      cache = new NotebookCohortCache();
      cache.setUserWorkspaceId(workspaceUserRole);
      cache.setCohortId(cohortId);
      cache.setLastAccessTime(lastAccessDate);
      cache.setNotebookName(null);
    }
    cache.setLastAccessTime(lastAccessDate);
    getDao().save(cache);
  }

  @Override
  public void deleteNotebook(Workspace workspace, User user, String notebookName) {
    WorkspaceUserRole workspaceUserRole =
        getworkspaceUserRoleDao().findWorkspaceUserRolesByWorkspaceAndUser(workspace, user);
    if (workspaceUserRole == null) {
      throw new BadRequestException(String.format(
          "User %s with workspace %s not found.", workspace.getName(), user.getUserId()));
    }
    getDao().
        deleteNotebookCohortCacheByUserWorkspaceIdAndNotebookName(workspaceUserRole, notebookName);
  }

  //Delete the last access row for given workspace and user
  private void handleWorkspaceUserLimitInCache(WorkspaceUserRole user_workspace) {
    long count = getDao().countNotebookCohortCachesByUserWorkspaceId(user_workspace);
    if (count == configProvider.get().notebookCohortCacheConfig.count) {
      NotebookCohortCache cache = getDao()
          .findTopByUserWorkspaceIdOrderByLastAccessTime(user_workspace);
      getDao().delete(cache);
    }
  }
}

