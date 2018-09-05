package org.pmiops.workbench.db.dao;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.db.model.Cohort;
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

  @Autowired
  CohortDao cohortDao;

  public UserRecentResourceDao getDao() {
    return userRecentResourceDao;
  }

  @VisibleForTesting
  public void setDao(UserRecentResourceDao dao) {
    this.userRecentResourceDao = dao;
  }

  @VisibleForTesting
  public void setCohortDao(CohortDao cohortDao) {
    this.cohortDao = cohortDao;
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
  public UserRecentResource updateNotebookEntry(long workspaceId, long userId, String notebookNameWithPath,
      Timestamp lastAccessDateTime) {
    UserRecentResource recentResource = getDao().findByUserIdAndWorkspaceIdAndNotebookName(userId, workspaceId, notebookNameWithPath);
    if (recentResource == null) {
      handleUserLimit(userId);
      recentResource = new UserRecentResource();
      recentResource.setUserId(userId);
      recentResource.setWorkspaceId(workspaceId);
      recentResource.setCohort(null);
      recentResource.setNotebookName(notebookNameWithPath);
    }
    recentResource.setLastAccessDate(lastAccessDateTime);
    getDao().save(recentResource);
    return recentResource;
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
    Cohort cohort = cohortDao.findOne(cohortId);
    UserRecentResource resource = getDao().findByUserIdAndWorkspaceIdAndCohort(userId, workspaceId, cohort);
    if (resource == null) {
      handleUserLimit(userId);
      resource = new UserRecentResource();
      resource.setUserId(userId);
      resource.setWorkspaceId(workspaceId);
      resource.setCohort(cohort);
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

