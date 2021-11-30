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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {

  private static final Logger logger = Logger.getLogger(UserMetricsController.class.getName());

  private static final int MAX_RECENT_NOTEBOOKS = 8;
  private static final Logger log = Logger.getLogger(UserMetricsController.class.getName());
  private final Provider<DbUser> userProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final FireCloudService fireCloudService;
  private final CloudStorageClient cloudStorageClient;
  private final CommonMappers commonMappers;
  private FirecloudMapper firecloudMapper;
  private int distinctWorkspaceLimit = 5;

  // TODO(jaycarlton): migrate these private functions to MapStruct
  // Converts DB model to client Model
  // RW-5638
  private final Function<DbUserRecentResource, WorkspaceResource> TO_CLIENT =
      userRecentResource -> {
        WorkspaceResource resource = new WorkspaceResource();
        resource.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.getCohort()));
        resource.setConceptSet(TO_CLIENT_CONCEPT_SET.apply(userRecentResource.getConceptSet()));
        FileDetail fileDetail = convertStringToFileDetail(userRecentResource.getNotebookName());
        resource.setNotebook(fileDetail);
        resource.setLastModifiedEpochMillis(userRecentResource.getLastAccessDate().getTime());
        resource.setWorkspaceId(userRecentResource.getWorkspaceId());
        return resource;
      };

  private static final Function<DbCohort, Cohort> TO_CLIENT_COHORT =
      cohort -> {
        if (cohort == null) {
          return null;
        }
        Cohort result =
            new Cohort()
                .etag(Etags.fromVersion(cohort.getVersion()))
                .lastModifiedTime(cohort.getLastModifiedTime().getTime())
                .creationTime(cohort.getCreationTime().getTime())
                .criteria(cohort.getCriteria())
                .description(cohort.getDescription())
                .id(cohort.getCohortId())
                .name(cohort.getName())
                .type(cohort.getType());
        if (cohort.getCreator() != null) {
          result.setCreator(cohort.getCreator().getUsername());
        }
        return result;
      };

  private static final Function<DbConceptSet, ConceptSet> TO_CLIENT_CONCEPT_SET =
      conceptSet -> {
        if (conceptSet == null) {
          return null;
        }
        ConceptSet result =
            new ConceptSet()
                .domain(conceptSet.getDomainEnum())
                .etag(Etags.fromVersion(conceptSet.getVersion()))
                .lastModifiedTime(conceptSet.getLastModifiedTime().getTime())
                .creationTime(conceptSet.getCreationTime().getTime())
                .description(conceptSet.getDescription())
                .id(conceptSet.getConceptSetId())
                .name(conceptSet.getName());
        return result;
      };

  @Autowired
  UserMetricsController(
      Provider<DbUser> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceDao workspaceDao,
      WorkspaceAuthService workspaceAuthService,
      FireCloudService fireCloudService,
      CloudStorageClient cloudStorageClient,
      CommonMappers commonMappers,
      FirecloudMapper firecloudMapper) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceDao = workspaceDao;
    this.workspaceAuthService = workspaceAuthService;
    this.fireCloudService = fireCloudService;
    this.cloudStorageClient = cloudStorageClient;
    this.commonMappers = commonMappers;
    this.firecloudMapper = firecloudMapper;
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
    // this is only ever used for Notebooks because we update/add to the cache for the other
    // resources in the backend
    // Because we don't store notebooks in our database the way we do other resources.
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    String notebookPath;
    if (recentResourceRequest.getNotebookName().startsWith("gs://")) {
      notebookPath = recentResourceRequest.getNotebookName();
    } else {
      String bucket =
          fireCloudService
              .getWorkspace(workspaceNamespace, workspaceId)
              .getWorkspace()
              .getBucketName();
      notebookPath = "gs://" + bucket + "/notebooks/" + recentResourceRequest.getNotebookName();
    }
    DbUserRecentResource recentResource =
        userRecentResourceService.updateNotebookEntry(
            wId, userProvider.get().getUserId(), notebookPath);
    return ResponseEntity.ok(TO_CLIENT.apply(recentResource));
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
    List<DbUserRecentResource> userRecentResourceList =
        userRecentResourceService.findAllResourcesByUser(userId);
    List<Long> workspaceIdList =
        userRecentResourceList.stream()
            .map(DbUserRecentResource::getWorkspaceId)
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

    final ImmutableList<DbUserRecentResource> workspaceFilteredResources =
        userRecentResourceList.stream()
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
                .map(DbUserRecentResource::getNotebookName)
                .map(this::uriToBlobId)
                .flatMap(Streams::stream)
                .limit(MAX_RECENT_NOTEBOOKS)
                .collect(Collectors.toList()));

    final ImmutableList<WorkspaceResource> userVisibleRecentResources =
        workspaceFilteredResources.stream()
            .filter(urr -> foundBlobIdsContainsUserRecentResource(foundBlobIds, urr))
            .map(urr -> buildRecentResource(idToDbWorkspace, idToFirecloudWorkspace, urr))
            .collect(ImmutableList.toImmutableList());
    final WorkspaceResourceResponse recentResponse = new WorkspaceResourceResponse();
    recentResponse.addAll(userVisibleRecentResources);

    return ResponseEntity.ok(recentResponse);
  }

  private Boolean foundBlobIdsContainsUserRecentResource(
      Set<BlobId> foundNotebooks, DbUserRecentResource urr) {
    return Optional.ofNullable(urr.getNotebookName())
        .flatMap(this::uriToBlobId)
        .map(foundNotebooks::contains)
        .orElse(true);
  }

  @VisibleForTesting
  public boolean hasValidBlobIdIfNotebookNamePresent(DbUserRecentResource dbUserRecentResource) {
    return Optional.ofNullable(dbUserRecentResource.getNotebookName())
        .map(name -> uriToBlobId(name).isPresent())
        .orElse(true);
  }

  // TODO RW-5638 reimplement all this in MapStruct
  private WorkspaceResource buildRecentResource(
      Map<Long, DbWorkspace> idToDbWorkspace,
      Map<Long, FirecloudWorkspaceResponse> idToFcWorkspaceResponse,
      DbUserRecentResource dbUserRecentResource) {

    WorkspaceResource resource = TO_CLIENT.apply(dbUserRecentResource);
    final long workspaceId = dbUserRecentResource.getWorkspaceId();

    buildFromDbWorkspace(resource, idToDbWorkspace.get(workspaceId));
    buildFromFcWorkspace(resource, idToFcWorkspaceResponse.get(workspaceId));

    return resource;
  }

  private void buildFromDbWorkspace(WorkspaceResource resource, DbWorkspace dbWorkspace) {
    resource.setCdrVersionId(commonMappers.cdrVersionToId(dbWorkspace.getCdrVersion()));
    resource.setAccessTierShortName(dbWorkspace.getCdrVersion().getAccessTier().getShortName());
    resource.setWorkspaceBillingStatus(dbWorkspace.getBillingStatus());
    resource.setAdminLocked(dbWorkspace.isAdminLocked());
  }

  private void buildFromFcWorkspace(
      WorkspaceResource resource, FirecloudWorkspaceResponse workspaceDetails) {
    resource.setPermission(
        firecloudMapper.fcToApiWorkspaceAccessLevel(workspaceDetails.getAccessLevel()).toString());
    resource.setWorkspaceNamespace(workspaceDetails.getWorkspace().getNamespace());
    resource.setWorkspaceFirecloudName(workspaceDetails.getWorkspace().getName());
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
