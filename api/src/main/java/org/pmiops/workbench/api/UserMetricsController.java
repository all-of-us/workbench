package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {
  private static final Logger log = Logger.getLogger(UserMetricsController.class.getName());
  private Provider<User> userProvider;
  private UserRecentResourceService userRecentResourceService;
  private WorkspaceService workspaceService;
  private FireCloudService fireCloudService;
  private int distinctWorkspacelimit = 5;
  private Clock clock;

  //Converts DB model to client Model
  private final Function<UserRecentResource, RecentResource> TO_CLIENT =
      userRecentResource -> {
        RecentResource resource = new RecentResource();
        resource.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.getCohort()));
        FileDetail fileDetail = convertStringToFileDetail(userRecentResource.getNotebookName());
        resource.setNotebook(fileDetail);
        resource.setModifiedTime(userRecentResource.getLastAccessDate().toString());
        resource.setWorkspaceId(userRecentResource.getWorkspaceId());
        return resource;
      };

  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          if (cohort == null) {
            return null;
          }
          Cohort result = new Cohort()
              .etag(Etags.fromVersion(cohort.getVersion()))
              .lastModifiedTime(cohort.getLastModifiedTime().getTime())
              .creationTime(cohort.getCreationTime().getTime())
              .criteria(cohort.getCriteria())
              .description(cohort.getDescription())
              .id(cohort.getCohortId())
              .name(cohort.getName())
              .type(cohort.getType());
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
          return result;
        }
      };

  @Autowired
  UserMetricsController(Provider<User> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Clock clock) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.clock = clock;
  }

  @VisibleForTesting
  public void setDistinctWorkspaceLimit(int limit) {
    distinctWorkspacelimit = limit;
  }


  @Override
  public ResponseEntity<RecentResource> updateRecentResource(String workspaceNamespace, String workspaceId, RecentResourceRequest recentResourceRequest) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    String notebookPath =  new String();
    if (recentResourceRequest.getNotebookName().startsWith("gs://")) {
      notebookPath = recentResourceRequest.getNotebookName();
    } else {
      String bucket = fireCloudService.getWorkspace(workspaceNamespace, workspaceId)
      .getWorkspace()
      .getBucketName();
      notebookPath = "gs://" + bucket + "/notebooks/" + recentResourceRequest.getNotebookName();
    }
    UserRecentResource recentResource = userRecentResourceService.updateNotebookEntry(wId, userProvider.get().getUserId(), notebookPath, now);
    return ResponseEntity.ok(TO_CLIENT.apply(recentResource));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRecentResource(String workspaceNamespace, String workspaceId, RecentResourceRequest recentResourceRequest) {
    long wId = getWorkspaceId(workspaceNamespace, workspaceId);
    userRecentResourceService.deleteNotebookEntry(wId, userProvider.get().getUserId(), recentResourceRequest.getNotebookName());
    return ResponseEntity.ok(new EmptyResponse());
  }

  /**
   * Gets the list of all resources recently access by user in order of access date time
   *
   * @return
   */
  @Override
  public ResponseEntity<RecentResourceResponse> getUserRecentResources() {
    long userId = userProvider.get().getUserId();
    RecentResourceResponse recentResponse = new RecentResourceResponse();
    List<UserRecentResource> userRecentResourceList = userRecentResourceService.findAllResourcesByUser(userId);
    List<Long> workspaceIdList = userRecentResourceList
        .stream()
        .map(UserRecentResource::getWorkspaceId)
        .distinct()
        .limit(distinctWorkspacelimit)
        .collect(Collectors.toList());

    // RW-1298
    // This needs to be refactored to only use namespace and FC ID
    // The purpose of this Map, is to check what is actually still present in FC 
    Map<Long, WorkspaceResponse> workspaceAccessMap = workspaceIdList.stream().collect(Collectors.toMap(id -> id, id -> {
      Workspace workspace = workspaceService.findByWorkspaceId((long) id);
      WorkspaceResponse workspaceResponse = fireCloudService
          .getWorkspace(workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName());
      return workspaceResponse;
    }));

    userRecentResourceList.stream()
        .filter(userRecentResource -> {
          return workspaceAccessMap.containsKey(userRecentResource.getWorkspaceId());
        })
        .forEach(userRecentResource -> {
          RecentResource resource = TO_CLIENT.apply(userRecentResource);
          WorkspaceResponse workspaceDetails = workspaceAccessMap.get(userRecentResource.getWorkspaceId());
          resource.setPermission(workspaceDetails.getAccessLevel());
          resource.setWorkspaceNamespace(workspaceDetails.getWorkspace().getNamespace());
          resource.setWorkspaceFirecloudName(workspaceDetails.getWorkspace().getName());
          recentResponse.add(resource);
        });
    return ResponseEntity.ok(recentResponse);
  }

  //Retrieves Database workspace ID
  private long getWorkspaceId(String workspaceNamespace, String workspaceId) {
    Workspace dbWorkspace = workspaceService.getRequired(workspaceNamespace, workspaceId);
    return dbWorkspace.getWorkspaceId();
  }

  private FileDetail convertStringToFileDetail(String str) {
    if (str != null && str.startsWith("gs://")) {
      int pos = str.lastIndexOf('/') + 1;
      String fileName = str.substring(pos);
      String replacement = Matcher.quoteReplacement(fileName) + "$";
      String filePath = str.replaceFirst(replacement, "");
      return new FileDetail().name(fileName).path(filePath);
    }
    log.log(Level.SEVERE, String.format("Invalid notebook file path found: %s", str));
    return null;
  }

}
