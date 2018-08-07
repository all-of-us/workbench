package org.pmiops.workbench.db.dao;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.sql.Timestamp;

@Service
public class UserRecentResourceServiceImpl implements UserRecentResourceService {

  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  UserRecentResourceDao userRecentResourceDao;

  public UserRecentResourceServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  public UserRecentResourceDao getDao() {
    return userRecentResourceDao;
  }

  @VisibleForTesting
  public void setDao(UserRecentResourceDao dao) {
    this.userRecentResourceDao = dao;
  }

  /**
   * Checks if notebook for given workspace and user is already in  table user_recent_resource
   * if yes, update the lastmodified time only
   * If no, check the number of resource entries for given  user
   * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new entry
   *
   * @param workspaceId
   * @param userId
   * @param notebookName
   * @param lastAccessDateTime
   */
  @Override
  public void updateNotebookEntry(long workspaceId, long userId, String notebookName,
      Timestamp lastAccessDateTime) {
    UserRecentResource resource = getDao().findByUserIdAndWorkspaceIdAndNotebookName(userId, workspaceId, notebookName);
    if (resource == null) {
      handleUserLimit(userId);
      resource = new UserRecentResource();
      resource.setUserId(userId);
      resource.setWorkspaceId(workspaceId);
      resource.setCohortId(null);
      resource.setNotebookName(notebookName);
    }
    resource.setLastAccessTime(lastAccessDateTime);
    getDao().save(resource);
  }

  /**
   * Checks if cohort for given workspace and user is already in table user_recent_resource
   * if yes, update the lastmodified time only
   * If no, check the number of resource entries for given  user
   * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new entry
   *
   * @param workspaceId
   * @param userId
   * @param cohortId
   * @param lastAccessDateTime
   */
  @Override
  public void updateCohortEntry(long workspaceId, long userId, long cohortId,
      Timestamp lastAccessDateTime) {
    UserRecentResource resource = getDao().findByUserIdAndWorkspaceIdAndCohortId(userId, workspaceId, cohortId);
    if (resource == null) {
      handleUserLimit(userId);
      resource = new UserRecentResource();
      resource.setUserId(userId);
      resource.setWorkspaceId(workspaceId);
      resource.setCohortId(cohortId);
      resource.setNotebookName(null);
    }
    resource.setLastAccessTime(lastAccessDateTime);
    getDao().save(resource);
  }

  /**
   * Deletes notebook entry from user_recent_resource
   *
   * @param workspaceId
   * @param userId
   * @param notebookName
   */
  @Override
  public void deleteNotebookEntry(long workspaceId, long userId, String notebookName) {
    getDao().deleteUserRecentResourceByUserIdAndWorkspaceIdAndNotebookName(workspaceId, userId, notebookName);
  }

  //Check number of entries in user_recent_resource for user, delete entries with least
  // lastAccessTime if count exceeds config entry userLimit
  private void handleUserLimit(long userId) {
    long count = getDao().countUserRecentResourceByUserId(userId);
    while (count-- >= configProvider.get().userRecentResourceConfig.userEntrycount) {
      UserRecentResource resource = getDao().findTopByUserIdOrderByLastAccessTime(userId);
      getDao().delete(resource);
    }
  }
}

