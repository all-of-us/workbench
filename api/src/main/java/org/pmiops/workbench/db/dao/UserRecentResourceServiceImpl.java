package org.pmiops.workbench.db.dao;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class UserRecentResourceServiceImpl implements UserRecentResourceService {

  private  final static int USER_ENTRY_COUNT = 10;

  @Autowired
  UserRecentResourceDao userRecentResourceDao;

  public UserRecentResourceDao getDao() {
    return userRecentResourceDao;
  }

  @VisibleForTesting
  public void setDao(UserRecentResourceDao dao) {
    this.userRecentResourceDao = dao;
  }

  @VisibleForTesting
  public int getUserEntryCount() {return USER_ENTRY_COUNT;}

  /**
   * Checks if notebook for given workspace and user is already in  table user_recent_resource
   * if yes, update the lastAccessDateTime only
   * If no, check the number of resource entries for given  user
   * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new entry
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
    resource.setLastAccessDate(lastAccessDateTime);
    getDao().save(resource);
  }

  /**
   * Checks if cohort for given workspace and user is already in table user_recent_resource
   * if yes, update the lastAccessDateTime only
   * If no, check the number of resource entries for given  user
   * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new entry
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
    resource.setLastAccessDate(lastAccessDateTime);
    getDao().save(resource);
  }

  /**
   * Deletes notebook entry from user_recent_resource
   */
  @Override
  public void deleteNotebookEntry(long workspaceId, long userId, String notebookName) {
    getDao().deleteUserRecentResourceByUserIdAndWorkspaceIdAndNotebookName(workspaceId, userId, notebookName);
  }

  /**
   * Retrieves the list of all resources recently accessed by user in descending order of last access date
   * @param userId : User id for whom the resources are returned
   */
  @Override
  public List<UserRecentResource> findAllResourcesByUser(long userId) {
    return getDao().findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(userId);
  }

  /**
   * Check number of entries in user_recent_resource for user,
   * If it exceeds USER_ENTRY_COUNT, delete the one with earliest lastAccessTime
   */
  private void handleUserLimit(long userId) {
    long count = getDao().countUserRecentResourceByUserId(userId);
    while (count-- >= USER_ENTRY_COUNT) {
      UserRecentResource resource = getDao().findTopByUserIdOrderByLastAccessDate(userId);
      getDao().delete(resource);
    }
  }
}

