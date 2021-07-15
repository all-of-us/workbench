package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.db.DbRetryUtils;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRecentResourceServiceImpl implements UserRecentResourceService {

  private Clock clock;
  private CohortDao cohortDao;
  private ConceptSetDao conceptSetDao;
  private UserRecentResourceDao userRecentResourceDao;

  @Autowired
  public UserRecentResourceServiceImpl(
      Clock clock,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      UserRecentResourceDao userRecentResourceDao) {
    this.clock = clock;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.userRecentResourceDao = userRecentResourceDao;
  }

  /**
   * Checks if notebook for given workspace and user is already in table user_recent_resource if
   * yes, update the lastAccessDateTime only If no, check the number of resource entries for given
   * user if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add
   * a new entry
   */
  @Override
  public DbUserRecentResource updateNotebookEntry(
      long workspaceId, long userId, String notebookNameWithPath) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    DbUserRecentResource recentResource =
        userRecentResourceDao.findByUserIdAndWorkspaceIdAndNotebookName(
            userId, workspaceId, notebookNameWithPath);
    if (recentResource == null) {
      handleUserLimit(userId);
      recentResource = new DbUserRecentResource(workspaceId, userId, notebookNameWithPath, now);
    }
    recentResource.setLastAccessDate(now);
    return userRecentResourceDao.save(recentResource);
  }

  /**
   * Checks if cohort for given workspace and user is already in table user_recent_resource if yes,
   * update the lastAccessDateTime only If no, check the number of resource entries for given user
   * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new
   * entry
   */
  @Override
  public void updateCohortEntry(long workspaceId, long userId, long cohortId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    DbCohort cohort = cohortDao.findById(cohortId).orElse(null);
    DbUserRecentResource resource =
        userRecentResourceDao.findByUserIdAndWorkspaceIdAndCohort(userId, workspaceId, cohort);
    if (resource == null) {
      handleUserLimit(userId);
      resource = new DbUserRecentResource(workspaceId, userId, now);
      resource.setCohort(cohort);
    }
    resource.setLastAccessDate(now);
    userRecentResourceDao.save(resource);
  }

  @Override
  public void updateConceptSetEntry(long workspaceId, long userId, long conceptSetId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    final DbConceptSet conceptSet = conceptSetDao.findById(conceptSetId).orElse(null);
    userRecentResourceDao.save(makeUserRecentResource(workspaceId, userId, now, conceptSet));
  }

  @NotNull
  private DbUserRecentResource makeUserRecentResource(
      long workspaceId, long userId, Timestamp now, DbConceptSet conceptSet) {
    DbUserRecentResource resource =
        userRecentResourceDao.findByUserIdAndWorkspaceIdAndConceptSet(
            userId, workspaceId, conceptSet);
    if (resource == null) {
      handleUserLimit(userId);
      resource = new DbUserRecentResource(workspaceId, userId, now);
      resource.setConceptSet(conceptSet);
    }
    resource.setLastAccessDate(now);
    return resource;
  }

  /** Deletes notebook entry from user_recent_resource */
  @Override
  public void deleteNotebookEntry(long workspaceId, long userId, String notebookPath) {
    DbUserRecentResource resource =
        userRecentResourceDao.findByUserIdAndWorkspaceIdAndNotebookName(
            userId, workspaceId, notebookPath);
    if (resource != null) {
      userRecentResourceDao.delete(resource);
    }
  }

  /**
   * Retrieves the list of all resources recently accessed by user in descending order of last
   * access date. This list is not filtered by visibility of these resources (for example, it may
   * contain resources in an inactive workspace.
   *
   * @param userId : User id for whom the resources are returned
   */
  @Override
  public List<DbUserRecentResource> findAllResourcesByUser(long userId) {
    try {
      return DbRetryUtils.executeAndRetry(
          () ->
              userRecentResourceDao.findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(
                  userId),
          Duration.ofSeconds(1),
          5);
    } catch (InterruptedException e) {
      throw new ServerErrorException("Unable to find Resources for user" + userId);
    }
  }

  /**
   * Check number of entries in user_recent_resource for user, If it exceeds USER_ENTRY_COUNT,
   * delete the one with earliest lastAccessTime
   */
  private void handleUserLimit(long userId) {
    long count = userRecentResourceDao.countUserRecentResourceByUserId(userId);
    while (count-- >= USER_ENTRY_COUNT) {
      DbUserRecentResource resource =
          userRecentResourceDao.findTopByUserIdOrderByLastAccessDate(userId);
      userRecentResourceDao.delete(resource);
    }
  }
}
