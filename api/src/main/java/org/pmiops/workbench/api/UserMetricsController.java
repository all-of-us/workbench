package org.pmiops.workbench.api;

import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {
   Provider<User> userProvider;
  UserRecentResourceService userRecentResourceService;
  WorkspaceService workspaceService;
  FireCloudService fireCloudService;
  Map<Long, String> workspaceAccessMap = new HashMap<Long, String>();
  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks/";

  @Autowired
  UserMetricsController(Provider<User> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
  }

  /**
   * Gets the list of all resources recently access by user in order of access date time
   * @return
   */
  @Override
  public ResponseEntity<RecentResourceResponse> getUserRecentResources() {
    long userId = userProvider.get().getUserId();
    RecentResourceResponse response = new RecentResourceResponse();
    List<UserRecentResource> userRecentResourceList = userRecentResourceService.findAllResourcesByUser(userId);
    List<Long> workspaceIdList = userRecentResourceList
        .stream()
        .map(UserRecentResource::getWorkspaceId)
        .distinct()
        .collect(Collectors.toList());

    workspaceAccessMap = workspaceIdList.stream().collect(Collectors.toMap(id -> id, id -> {
      Workspace workspace = workspaceService.findByWorkspaceId((long) id);
      WorkspaceResponse workspaceResponse = fireCloudService
          .getWorkspace(workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName());
      return workspaceResponse.getAccessLevel();
    }));

    userRecentResourceList.stream().forEach(userRecentResource -> {
      response.add(TO_CLIENT.apply(userRecentResource));
    });

    return ResponseEntity.ok(response);
  }

  //Converts DB model to client Model
  private final Function<UserRecentResource, RecentResource> TO_CLIENT =
      userRecentResource -> {
        RecentResource response = new RecentResource();
        response.setCohort(TO_CLIENT_COHORT.apply(userRecentResource.getCohort()));
        response.setPermission(workspaceAccessMap.get(userRecentResource.getWorkspaceId()));
        if(userRecentResource.getNotebookName() != null ) {
          FileDetail fileDetail = new FileDetail();
          String[] notebookDetails = userRecentResource.getNotebookName().split(NOTEBOOKS_WORKSPACE_DIRECTORY);
          fileDetail.setPath(notebookDetails[0]+NOTEBOOKS_WORKSPACE_DIRECTORY);
          fileDetail.setName(notebookDetails[1]);
          response.setNotebook(fileDetail);
        }
        response.setModifiedTime(userRecentResource.getLastAccessDate().toString());
        return response;
      };

  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          if(cohort == null)
            return null;
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
}


