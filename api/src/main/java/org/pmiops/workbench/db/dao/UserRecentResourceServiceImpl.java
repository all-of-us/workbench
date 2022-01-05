package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.db.DbRetryUtils;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbUserRecentResourcesId;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRecentResourceServiceImpl implements UserRecentResourceService {

  private Clock clock;
  private CohortDao cohortDao;
  private ConceptSetDao conceptSetDao;
  private UserRecentResourceDao userRecentResourceDao;
  private UserRecentResourcesIdDao userRecentResourcesIdDao;

  @Autowired
  public UserRecentResourceServiceImpl(
      Clock clock,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      UserRecentResourcesIdDao userRecentResourcesIdDao,
      UserRecentResourceDao userRecentResourceDao) {
    this.clock = clock;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.userRecentResourcesIdDao = userRecentResourcesIdDao;
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
    updateUserRecentResourceEntry(workspaceId, userId, ResourceType.NOTEBOOK, notebookNameWithPath);
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
    updateUserRecentResourceEntry(
        workspaceId, userId, ResourceType.COHORT, String.valueOf(cohortId));
  }

  @Override
  public void updateConceptSetEntry(long workspaceId, long userId, long conceptSetId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    final DbConceptSet conceptSet = conceptSetDao.findById(conceptSetId).orElse(null);
    userRecentResourceDao.save(makeUserRecentResource(workspaceId, userId, now, conceptSet));
    updateUserRecentResourceEntry(
        workspaceId, userId, ResourceType.CONCEPT_SET, String.valueOf(conceptSetId));
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

  private void updateUserRecentResourceEntry(
      long workspaceId, long userId, ResourceType resourceType, String resourceId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    DbUserRecentResourcesId recentResourcesId =
        userRecentResourcesIdDao
            .findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
                userId,
                workspaceId,
                DbStorageEnums.resourceTypeToStorage(resourceType),
                resourceId);
    if (recentResourcesId == null) {
      recentResourcesId =
          new DbUserRecentResourcesId(workspaceId, userId, resourceType, resourceId, now);
    }
    userRecentResourcesIdDao.save(recentResourcesId);
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
    deleteEntryFromUserRecentResource(userId, workspaceId, ResourceType.NOTEBOOK, notebookPath);
  }

  /** Deletes cohort entry from user_recent_resource */
  @Override
  public void deleteCohortEntry(long workspaceId, long userId, long cohortId) {
    deleteEntryFromUserRecentResource(
        userId, workspaceId, ResourceType.COHORT, String.valueOf(cohortId));
  }

  /** Deletes concept set entry from user_recent_resource */
  @Override
  public void deleteConceptSetEntry(long workspaceId, long userId, long conceptSetId) {
    deleteEntryFromUserRecentResource(
        userId, workspaceId, ResourceType.CONCEPT_SET, String.valueOf(conceptSetId));
  }

  private void deleteEntryFromUserRecentResource(
      long userId, long workspaceId, ResourceType resourceType, String resourceId) {
    DbUserRecentResourcesId resourceById =
        userRecentResourcesIdDao
            .findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
                userId,
                workspaceId,
                DbStorageEnums.resourceTypeToStorage(resourceType),
                resourceId);
    if (resourceById != null) {
      userRecentResourcesIdDao.delete(resourceById);
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

      DbUserRecentResourcesId resourceById =
          userRecentResourcesIdDao.findTopByUserIdOrderByLastAccessDate(userId);
      userRecentResourcesIdDao.delete(resourceById);
    }
  }
}
