package org.pmiops.workbench.api;

import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {

  private static final int MAX_RECENT_NOTEBOOKS = 8;

  private static final Logger log = Logger.getLogger(UserMetricsController.class.getName());

  private final CloudStorageClient cloudStorageClient;
  private final FireCloudService fireCloudService;
  private final Provider<DbUser> userProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceResourceMapper workspaceResourceMapper;
  private final ConceptSetService conceptSetService;
  private final DataSetService dataSetService;
  private final CohortService cohortService;

  private int distinctWorkspaceLimit = 5;

  @Autowired
  UserMetricsController(
      CloudStorageClient cloudStorageClient,
      CohortService cohortService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      Provider<DbUser> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceResourceMapper workspaceResourceMapper) {
    this.cloudStorageClient = cloudStorageClient;
    this.cohortService = cohortService;
    this.conceptSetService = conceptSetService;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceResourceMapper = workspaceResourceMapper;
  }

  @VisibleForTesting
  public void setDistinctWorkspaceLimit(int limit) {
    distinctWorkspaceLimit = limit;
  }

  @Override
  public ResponseEntity<WorkspaceResource> updateRecentResource(
      String workspaceNamespace, String workspaceId, RecentResourceRequest recentResourceRequest) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    final FirecloudWorkspaceResponse fcWorkspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId);

    final String notebookPath;
    if (recentResourceRequest.getNotebookName().startsWith("gs://")) {
      notebookPath = recentResourceRequest.getNotebookName();
    } else {
      String bucket = fcWorkspace.getWorkspace().getBucketName();
      notebookPath = "gs://" + bucket + "/notebooks/" + recentResourceRequest.getNotebookName();
    }

    // this is only ever used for Notebooks because we update/add to the cache for the other
    // resources in the backend
    // Because we don't store notebooks in our database the way we do other resources.
    final DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    final DbUserRecentResource recentResource =
        userRecentResourceService.updateNotebookEntry(
            dbWorkspace.getWorkspaceId(), userProvider.get().getUserId(), notebookPath);

    return ResponseEntity.ok(
        workspaceResourceMapper.fromDbUserRecentResource(recentResource, fcWorkspace, dbWorkspace));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRecentResource(
      String workspaceNamespace, String workspaceId, RecentResourceRequest recentResourceRequest) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    userRecentResourceService.deleteNotebookEntry(
        wId, userProvider.get().getUserId(), recentResourceRequest.getNotebookName());
    return ResponseEntity.ok(new EmptyResponse());
  }

  /** Gets the list of all resources recently access by user in order of access date time */
  @Override
  public ResponseEntity<WorkspaceResourceResponse> getUserRecentResources() {
    long userId = userProvider.get().getUserId();
    List<DbUserRecentlyModifiedResource> userRecentlyModifiedResourceList =
        userRecentResourceService.findAllRecentlyModifiedResourcesByUser(userId);

    List<Long> workspaceIdList =
        userRecentlyModifiedResourceList.stream()
            .map(DbUserRecentlyModifiedResource::getWorkspaceId)
            .distinct()
            .limit(distinctWorkspaceLimit)
            .collect(Collectors.toList());

    final Map<Long, DbWorkspace> idToDbWorkspace =
        workspaceIdList.stream()
            .map(
                id ->
                    workspaceDao
                        .findActiveByWorkspaceId(id)
                        .map(
                            dbWorkspace ->
                                new AbstractMap.SimpleImmutableEntry<>(
                                    dbWorkspace.getWorkspaceId(), dbWorkspace)))
            .flatMap(Streams::stream)
            .collect(
                ImmutableMap.toImmutableMap(
                    SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

    final Map<Long, FirecloudWorkspaceResponse> idToFirecloudWorkspace =
        idToDbWorkspace.entrySet().stream()
            .map(
                entry ->
                    fireCloudService
                        .getWorkspace(entry.getValue())
                        .map(
                            workspaceResponse ->
                                new AbstractMap.SimpleImmutableEntry<>(
                                    entry.getKey(), workspaceResponse)))
            .flatMap(Streams::stream)
            .collect(
                ImmutableMap.toImmutableMap(
                    SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

    final ImmutableList<DbUserRecentlyModifiedResource> workspaceFilteredResources =
        userRecentlyModifiedResourceList.stream()
            .filter(r -> idToFirecloudWorkspace.containsKey(r.getWorkspaceId()))
            .filter(this::hasValidBlobIdIfNotebookNamePresent)
            .collect(ImmutableList.toImmutableList());

    // Check for existence of recent notebooks. Notebooks reside in GCS so they may be arbitrarily
    // deleted or renamed without notification to the Workbench. This makes a batch of GCS requests
    // so it will scale fairly well, but limit to the first N notebooks to avoid excess GCS traffic.
    // TODO: If we find a non-existent notebook, expunge from the cache.
    // TODO(jaycarlton) I'm not sure whether it's right to do this here or in a cron job. I don't
    // personally like GET endpoints to have side effects, and besides, we're not touching enough
    // of the notebooks to keep the cache up-to-date from here.
    final Set<BlobId> foundBlobIds =
        cloudStorageClient.getExistingBlobIdsIn(
            workspaceFilteredResources.stream()
                .filter(
                    recentResource ->
                        recentResource.getResourceType()
                            == DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType
                                .NOTEBOOK)
                .map(DbUserRecentlyModifiedResource::getResourceId)
                .map(this::uriToBlobId)
                .flatMap(Streams::stream)
                .limit(MAX_RECENT_NOTEBOOKS)
                .collect(Collectors.toList()));

    final ImmutableList<WorkspaceResource> userVisibleRecentResources =
        workspaceFilteredResources.stream()
            .filter(urr -> foundBlobIdsContainsUserRecentlyModifiedResource(foundBlobIds, urr))
            .map(urr -> buildRecentResource(idToDbWorkspace, idToFirecloudWorkspace, urr))
            .collect(ImmutableList.toImmutableList());
    final WorkspaceResourceResponse recentResponse = new WorkspaceResourceResponse();
    recentResponse.addAll(userVisibleRecentResources);

    return ResponseEntity.ok(recentResponse);
  }

  private Boolean foundBlobIdsContainsUserRecentlyModifiedResource(
      Set<BlobId> foundNotebooks, DbUserRecentlyModifiedResource urr) {
    if (urr.getResourceType()
        == DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK) {
      return Optional.ofNullable(urr.getResourceId())
          .flatMap(this::uriToBlobId)
          .map(foundNotebooks::contains)
          .orElse(true);
    }
    return true;
  }

  @VisibleForTesting
  public boolean hasValidBlobIdIfNotebookNamePresent(
      DbUserRecentlyModifiedResource dbUserRecentResource) {
    if (dbUserRecentResource.getResourceType()
        == DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType.NOTEBOOK) {
      return Optional.ofNullable(dbUserRecentResource.getResourceId())
          .map(name -> uriToBlobId(name).isPresent())
          .orElse(true);
    }
    return true;
  }

  /**
   * Build recent resource object by grabbing the DB object (cohort/conceptSet or dataSet) depending
   * upon the DbUserRecentlyModifiedResource.ResourceType
   *
   * @param idToDbWorkspace
   * @param idToFcWorkspaceResponse
   * @param dbUserRecentlyModifiedResource
   * @return WorkspaceResource
   */
  private WorkspaceResource buildRecentResource(
      Map<Long, DbWorkspace> idToDbWorkspace,
      Map<Long, FirecloudWorkspaceResponse> idToFcWorkspaceResponse,
      DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource) {

    final long workspaceId = dbUserRecentlyModifiedResource.getWorkspaceId();
    FirecloudWorkspaceResponse firecloudWorkspaceResponse =
        idToFcWorkspaceResponse.get(workspaceId);
    DbWorkspace dbWorkspace = idToDbWorkspace.get(workspaceId);
    switch (dbUserRecentlyModifiedResource.getResourceType()) {
      case COHORT:
        return workspaceResourceMapper.fromDbUserRecentlyModifiedResourceAndCohort(
            dbUserRecentlyModifiedResource,
            firecloudWorkspaceResponse,
            dbWorkspace,
            cohortService.findDbCohortByCohortId(
                getResourceIdInLong(dbUserRecentlyModifiedResource)));
      case CONCEPT_SET:
        return workspaceResourceMapper.fromDbUserRecentlyModifiedResourceAndConceptSet(
            dbUserRecentlyModifiedResource,
            firecloudWorkspaceResponse,
            dbWorkspace,
            conceptSetService.getDbConceptSet(
                workspaceId, getResourceIdInLong(dbUserRecentlyModifiedResource)));
      case DATA_SET:
        return workspaceResourceMapper.fromDbUserRecentlyModifiedResourceAndDataSet(
            dbUserRecentlyModifiedResource,
            firecloudWorkspaceResponse,
            dbWorkspace,
            dataSetService
                .getDbDataSet(workspaceId, getResourceIdInLong(dbUserRecentlyModifiedResource))
                .orElse(new DbDataset()));
      case NOTEBOOK:
        return workspaceResourceMapper.fromDbUserRecentlyModifiedResourceAndNotebookName(
            dbUserRecentlyModifiedResource,
            firecloudWorkspaceResponse,
            dbWorkspace,
            dbUserRecentlyModifiedResource.getResourceId());
    }
    return null;
  }

  private Long getResourceIdInLong(DbUserRecentlyModifiedResource dbUserRecentlyModifiedResource) {
    return Long.parseLong(
        Optional.ofNullable(dbUserRecentlyModifiedResource.getResourceId()).orElse("0"));
  }

  // Retrieves Database workspace ID
  private long getWorkspaceId(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    return dbWorkspace.getWorkspaceId();
  }

  private Optional<BlobId> uriToBlobId(String uri) {
    if (uri == null) {
      return Optional.empty();
    }
    if (!uri.startsWith("gs://")) {
      log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", uri));
      return Optional.empty();
    }
    uri = uri.replaceFirst("gs://", "");
    String[] parts = uri.split("/");
    if (parts.length <= 1) {
      log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", uri));
      return Optional.empty();
    }
    final String name = Joiner.on('/').join(Arrays.copyOfRange(parts, 1, parts.length));
    return Optional.of(BlobId.of(parts[0], name));
  }

  private FileDetail convertStringToFileDetail(String str) {
    if (str == null) {
      return null;
    }
    if (!str.startsWith("gs://")) {
      log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", str));
      return null;
    }
    int filenameStart = str.lastIndexOf('/') + 1;
    return new FileDetail()
        .name(str.substring(filenameStart))
        .path(str.substring(0, filenameStart));
  }
}
