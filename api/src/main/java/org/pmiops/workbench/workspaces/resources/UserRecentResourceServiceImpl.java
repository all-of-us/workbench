package org.pmiops.workbench.workspaces.resources;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.DbRetryUtils;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserRecentlyModifiedResourceDao;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRecentResourceServiceImpl implements UserRecentResourceService {

  private Clock clock;
  private CohortReviewDao cohortReviewDao;
  private DataSetDao datasetDao;
  private UserRecentlyModifiedResourceDao userRecentlyModifiedResourceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public UserRecentResourceServiceImpl(
      Clock clock,
      CohortReviewDao cohortReviewDao,
      DataSetDao datasetDao,
      UserRecentlyModifiedResourceDao userRecentlyModifiedResourceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.clock = clock;
    this.cohortReviewDao = cohortReviewDao;
    this.datasetDao = datasetDao;
    this.userRecentlyModifiedResourceDao = userRecentlyModifiedResourceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Create a Notebook recent-resource entry in the DB if none exists, reducing the table size to
   * USER_ENTRY_COUNT per-user if necessary. Update the last accessed time if it does exist.
   */
  @Override
  public DbUserRecentlyModifiedResource updateNotebookEntry(
      long workspaceId, long userId, String notebookNameWithPath) {
    return updateUserRecentlyModifiedResourceEntry(
        workspaceId,
        userId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK,
        notebookNameWithPath);
  }

  /**
   * Create a Cohort recent-resource entry in the DB if none exists, reducing the table size to
   * USER_ENTRY_COUNT per-user if necessary. Update the last accessed time if it does exist.
   */
  @Override
  public void updateCohortEntry(long workspaceId, long userId, long cohortId) {
    updateUserRecentlyModifiedResourceEntry(
        workspaceId,
        userId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.COHORT,
        String.valueOf(cohortId));
  }

  /**
   * Create a Concept Set recent-resource entry in the DB if none exists, reducing the table size to
   * USER_ENTRY_COUNT per-user if necessary. Update the last accessed time if it does exist.
   */
  @Override
  public void updateConceptSetEntry(long workspaceId, long userId, long conceptSetId) {
    updateUserRecentlyModifiedResourceEntry(
        workspaceId,
        userId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.CONCEPT_SET,
        String.valueOf(conceptSetId));
  }

  /**
   * Create a Dataset recent-resource entry in the DB if none exists, reducing the table size to
   * USER_ENTRY_COUNT per-user if necessary. Update the last accessed time if it does exist.
   */
  @Override
  public void updateDataSetEntry(long workspaceId, long userId, long dataSetId) {
    updateUserRecentlyModifiedResourceEntry(
        workspaceId,
        userId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.DATA_SET,
        String.valueOf(dataSetId));
  }

  /**
   * Create a Cohort Review recent-resource entry in the DB if none exists, reducing the table size
   * to USER_ENTRY_COUNT per-user if necessary. Update the last accessed time if it does exist.
   */
  @Override
  public void updateCohortReviewEntry(long workspaceId, long userId, long cohortReviewId) {
    updateUserRecentlyModifiedResourceEntry(
        workspaceId,
        userId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.COHORT_REVIEW,
        String.valueOf(cohortReviewId));
  }

  private DbUserRecentlyModifiedResource updateUserRecentlyModifiedResourceEntry(
      long workspaceId,
      long userId,
      DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
      String resourceId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    DbUserRecentlyModifiedResource recentResource =
        userRecentlyModifiedResourceDao.getResource(userId, workspaceId, resourceType, resourceId);
    if (recentResource == null) {
      handleUserLimit(userId);
      recentResource =
          new DbUserRecentlyModifiedResource(workspaceId, userId, resourceType, resourceId, now);
    } else {
      recentResource.setLastAccessDate(now);
    }
    return userRecentlyModifiedResourceDao.save(recentResource);
  }

  /** Deletes notebook entry from user_recently_modified_resource */
  @Override
  public void deleteNotebookEntry(long workspaceId, long userId, String notebookPath) {
    deleteUserRecentlyModifiedResourceEntry(
        userId,
        workspaceId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK,
        notebookPath);
  }

  /**
   * When a Cohort recent-resource is deleted, delete all the entries of Cohort Reviews and Datasets
   * which reference the Cohort as well from recent_resource
   */
  @Override
  public void deleteCohortEntry(long workspaceId, long userId, long cohortId) {
    cohortReviewDao.findAllByCohortId(cohortId).stream()
        .map(DbCohortReview::getCohortReviewId)
        .forEach(id -> deleteCohortReviewEntry(workspaceId, userId, id));

    datasetDao.findDbDataSetsByCohortIdsAndWorkspaceId(cohortId, workspaceId).stream()
        .map(DbDataset::getDataSetId)
        .forEach(id -> deleteDataSetEntry(workspaceId, userId, id));

    deleteUserRecentlyModifiedResourceEntry(
        userId,
        workspaceId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.COHORT,
        String.valueOf(cohortId));
  }

  /**
   * When a Concept Set recent-resource is deleted, delete all the entries of Datasets which
   * reference the Cohort as well from recent_resource
   */
  @Override
  public void deleteConceptSetEntry(long workspaceId, long userId, long conceptSetId) {
    datasetDao.findDbDatasetsByConceptSetIdsAndWorkspaceId(conceptSetId, workspaceId).stream()
        .map(DbDataset::getDataSetId)
        .forEach(id -> deleteDataSetEntry(workspaceId, userId, id));

    deleteUserRecentlyModifiedResourceEntry(
        userId,
        workspaceId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.CONCEPT_SET,
        String.valueOf(conceptSetId));
  }

  @Override
  public void deleteDataSetEntry(long workspaceId, long userId, long dataSetId) {
    deleteUserRecentlyModifiedResourceEntry(
        userId,
        workspaceId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.DATA_SET,
        String.valueOf(dataSetId));
  }

  @Override
  public void deleteCohortReviewEntry(long workspaceId, long userId, long cohortReviewId) {
    deleteUserRecentlyModifiedResourceEntry(
        userId,
        workspaceId,
        DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.COHORT_REVIEW,
        String.valueOf(cohortReviewId));
  }

  private void deleteUserRecentlyModifiedResourceEntry(
      long userId,
      long workspaceId,
      DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
      String resourceId) {
    DbUserRecentlyModifiedResource resourceById =
        userRecentlyModifiedResourceDao.getResource(userId, workspaceId, resourceType, resourceId);
    if (resourceById != null) {
      userRecentlyModifiedResourceDao.delete(resourceById);
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
  public List<DbUserRecentlyModifiedResource> findAllRecentlyModifiedResourcesByUser(long userId) {
    try {
      return DbRetryUtils.executeAndRetry(
          () -> userRecentlyModifiedResourceDao.getAllForUser(userId), Duration.ofSeconds(1), 5);
    } catch (InterruptedException e) {
      throw new ServerErrorException(
          "Unable to find Recently Modified Resources for user" + userId);
    }
  }

  /**
   * Check number of entries in user_recently_modified_resource for user, If it exceeds or equals
   * USER_ENTRY_COUNT, delete the one with earliest lastAccessTime
   */
  private void handleUserLimit(long userId) {
    long count = userRecentlyModifiedResourceDao.countByUserId(userId);
    while (count-- >= USER_ENTRY_COUNT) {
      DbUserRecentlyModifiedResource resourceById =
          userRecentlyModifiedResourceDao.findTopByUserIdOrderByLastAccessDate(userId);
      userRecentlyModifiedResourceDao.delete(resourceById);
    }
  }
}
