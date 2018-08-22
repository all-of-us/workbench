package org.pmiops.workbench.api;

import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.util.List;
import java.util.function.Function;

@RestController
public class UserMetricsController implements UserMetricsApiDelegate {
  Provider<User> userProvider;
  UserRecentResourceService userRecentResourceService;
  CohortService cohortService;
  WorkspaceService workspaceService;
  FireCloudService fireCloudService;

  @Autowired
  UserMetricsController(Provider<User> userProvider,
      UserRecentResourceService userRecentResourceService,
      CohortService cohortService,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService) {
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.cohortService = cohortService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
  }

  /**
   * Gets the list of all resources recently access by user in order of access date time
   * @return
   */
  @Override
  public ResponseEntity<RecentResourceResponse> getUserMetrics() {
    long userId = userProvider.get().getUserId();
    RecentResourceResponse response = new RecentResourceResponse();
    List<UserRecentResource> userRecentResourceList = userRecentResourceService.findAllResourcesByUser(userId);

    userRecentResourceList.stream().forEach(userRecentResource -> {
      response.add(TO_CLIENT.apply(userRecentResource));
    });

    return ResponseEntity.ok(response);
  }

  //Converts DB model to client Model
  private final Function<UserRecentResource, RecentResource> TO_CLIENT =
      userRecentResource -> {
        RecentResource response = new RecentResource();
        response.setCohortId(userRecentResource.getCohortId());
        response.setModifiedTime(userRecentResource.getLastAccessDate().toString());
        if (userRecentResource.getCohortId() == null) {
          response.setName(userRecentResource.getNotebookName());
          response.setType("notebook");
          response.setDescription("");
        } else {
          response.setType("cohort");
          Cohort cohort = cohortService
              .findCohortByWorkspaceIdAndCohortId(response.getWorkspaceId(), response.getCohortId());
          response.setName(cohort.getName());
          response.setDescription(cohort.getDescription());
        }
        response.setWorkspaceId(userRecentResource.getWorkspaceId());
        Workspace workspace = workspaceService.findByWorkspaceId(response.getWorkspaceId());
        WorkspaceResponse workspaceResponse = fireCloudService
            .getWorkspace(workspace.getWorkspaceNamespace(),
                workspace.getFirecloudName());
        response.setPermission(workspaceResponse.getAccessLevel());
        return response;
      };
}


