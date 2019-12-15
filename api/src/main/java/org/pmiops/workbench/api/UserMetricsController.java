package org.pmiops.workbench.api;

import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
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
  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final CloudStorageService cloudStorageService;
  private int distinctWorkspacelimit = 5;
  private Clock clock;

  // TODO(jaycarlton): migrate these private functions to MapStruct
  // Converts DB model to client Model
  private final Function<DbUserRecentResource, RecentResource> TO_CLIENT =
      userRecentResource -> {
        RecentResource resource = new RecentResource();
        resource.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.getCohort()));
        resource.setConceptSet(TO_CLIENT_CONCEPT_SET.apply(userRecentResource.getConceptSet()));
        FileDetail fileDetail = convertStringToFileDetail(userRecentResource.getNotebookName());
        resource.setNotebook(fileDetail);
        resource.setModifiedTime(userRecentResource.getLastAccessDate().toString());
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
          result.setCreator(cohort.getCreator().getUserName());
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
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      CloudStorageService cloudStorageService,
      Clock clock) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.cloudStorageService = cloudStorageService;
    this.clock = clock;
  }

  @VisibleForTesting
  public void setDistinctWorkspaceLimit(int limit) {
    distinctWorkspacelimit = limit;
  }

  @Override
  public ResponseEntity<RecentResource> updateRecentResource(
      String workspaceNamespace, String workspaceId, RecentResourceRequest recentResourceRequest) {
    // this is only ever used for Notebooks because we update/add to the cache for the other
    // resources in the backend
    // Because we don't store notebooks in our database the way we do other resources.
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
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
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    userRecentResourceService.deleteNotebookEntry(
        wId, userProvider.get().getUserId(), recentResourceRequest.getNotebookName());
    return ResponseEntity.ok(new EmptyResponse());
  }

  /** Gets the list of all resources recently access by user in order of access date time */
  @Override
  public ResponseEntity<RecentResourceResponse> getUserRecentResources() {
    long userId = userProvider.get().getUserId();
    List<DbUserRecentResource> userRecentResourceList =
        userRecentResourceService.findAllResourcesByUser(userId);
    List<Long> workspaceIdList =
        userRecentResourceList.stream()
            .map(DbUserRecentResource::getWorkspaceId)
            .distinct()
            .limit(distinctWorkspacelimit)
            .collect(Collectors.toList());

    final ImmutableMap<Long, FirecloudWorkspaceResponse> idToLiveWorkspace =
        workspaceIdList.stream()
            .map(
                id ->
                    workspaceService
                        .findActiveByWorkspaceId(id)
                        .map(
                            dbWorkspace ->
                                new AbstractMap.SimpleImmutableEntry<>(
                                    dbWorkspace.getWorkspaceId(), dbWorkspace)))
            .flatMap(Streams::stream)
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
            .filter(r -> idToLiveWorkspace.containsKey(r.getWorkspaceId()))
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
        cloudStorageService.getExistingBlobIdsIn(
            workspaceFilteredResources.stream()
                .map(DbUserRecentResource::getNotebookName)
                .map(this::uriToBlobId)
                .flatMap(Streams::stream)
                .limit(MAX_RECENT_NOTEBOOKS)
                .collect(Collectors.toList()));

    final ImmutableList<RecentResource> userVisibleRecentResources =
        workspaceFilteredResources.stream()
            .filter(urr -> foundBlobIdsContainsUserRecentResource(foundBlobIds, urr))
            .map(urr -> buildRecentResource(idToLiveWorkspace, urr))
            .collect(ImmutableList.toImmutableList());
    final RecentResourceResponse recentResponse = new RecentResourceResponse();
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

  private RecentResource buildRecentResource(
      ImmutableMap<Long, FirecloudWorkspaceResponse> idToFcWorkspaceResponse,
      DbUserRecentResource dbUserRecentResource) {
    RecentResource resource = TO_CLIENT.apply(dbUserRecentResource);
    FirecloudWorkspaceResponse workspaceDetails =
        idToFcWorkspaceResponse.get(dbUserRecentResource.getWorkspaceId());
    resource.setPermission(workspaceDetails.getAccessLevel());
    resource.setWorkspaceNamespace(workspaceDetails.getWorkspace().getNamespace());
    resource.setWorkspaceFirecloudName(workspaceDetails.getWorkspace().getName());
    return resource;
  }

  // Retrieves Database workspace ID
  private long getWorkspaceId(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
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
    int pos = str.lastIndexOf('/') + 1;
    String fileName = str.substring(pos);
    String replacement = Matcher.quoteReplacement(fileName) + "$";
    String filePath = str.replaceFirst(replacement, "");
    return new FileDetail().name(fileName).path(filePath);
  }
}
