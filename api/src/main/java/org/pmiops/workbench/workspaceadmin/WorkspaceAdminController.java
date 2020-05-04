package org.pmiops.workbench.workspaceadmin;

import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.WorkspaceAdminApiDelegate;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceAdminController implements WorkspaceAdminApiDelegate {

  private static final Duration TRAILING_TIME_TO_QUERY = Duration.ofHours(6);

  private final CloudMonitoringService cloudMonitoringService;
  private FirecloudMapper firecloudMapper;
  private final FireCloudService fireCloudService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private UserMapper userMapper;
  private UserService userService;
  private final WorkspaceAdminService workspaceAdminService;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspaceAdminController(
      CloudMonitoringService cloudMonitoringService,
      FirecloudMapper firecloudMapper,
      FireCloudService fireCloudService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      UserMapper userMapper,
      UserService userService,
      WorkspaceAdminService workspaceAdminService,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService) {
    this.cloudMonitoringService = cloudMonitoringService;
    this.firecloudMapper = firecloudMapper;
    this.fireCloudService = fireCloudService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userMapper = userMapper;
    this.userService = userService;
    this.workspaceAdminService = workspaceAdminService;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
  }

  @Override
  @AuthorityRequired({Authority.WORKSPACES_VIEW})
  public ResponseEntity<CloudStorageTraffic> getCloudStorageTraffic(String workspaceNamespace) {
    CloudStorageTraffic response = new CloudStorageTraffic().receivedBytes(new ArrayList<>());

    for (TimeSeries timeSeries :
        cloudMonitoringService.getCloudStorageReceivedBytes(
            workspaceNamespace, TRAILING_TIME_TO_QUERY)) {
      for (Point point : timeSeries.getPointsList()) {
        response.addReceivedBytesItem(
            new TimeSeriesPoint()
                .timestamp(Timestamps.toMillis(point.getInterval().getEndTime()))
                .value(point.getValue().getDoubleValue()));
      }
    }

    // Highcharts expects its data to be pre-sorted; we do this on the server side for convenience.
    response.getReceivedBytes().sort(Comparator.comparing(TimeSeriesPoint::getTimestamp));

    return ResponseEntity.ok(response);
  }

  @Override
  @AuthorityRequired({Authority.WORKSPACES_VIEW})
  public ResponseEntity<AdminFederatedWorkspaceDetailsResponse> getFederatedWorkspaceDetails(
      String workspaceNamespace) {
    final Optional<DbWorkspace> workspaceMaybe =
        workspaceAdminService.getFirstWorkspaceByNamespace(workspaceNamespace);
    if (workspaceMaybe.isPresent()) {
      final DbWorkspace dbWorkspace = workspaceMaybe.get();

      final String workspaceFirecloudName = dbWorkspace.getFirecloudName();

      final List<WorkspaceUserAdminView> collaborators =
          workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName)
              .stream()
              .map(this::toAdminView)
              .collect(Collectors.toList());

      final AdminWorkspaceObjectsCounts adminWorkspaceObjects =
          workspaceAdminService.getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId());

      final AdminWorkspaceCloudStorageCounts adminWorkspaceCloudStorageCounts =
          workspaceAdminService.getAdminWorkspaceCloudStorageCounts(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

      final List<ListClusterResponse> workbenchListClusterResponses =
          leonardoNotebooksClient.listClustersByProjectAsService(workspaceNamespace).stream()
              .map(firecloudMapper::toApiListClusterResponse)
              .collect(Collectors.toList());

      final AdminWorkspaceResources adminWorkspaceResources =
          new AdminWorkspaceResources()
              .workspaceObjects(adminWorkspaceObjects)
              .cloudStorage(adminWorkspaceCloudStorageCounts)
              .clusters(workbenchListClusterResponses);

      final FirecloudWorkspace firecloudWorkspace =
          fireCloudService
              .getWorkspaceAsService(workspaceNamespace, workspaceFirecloudName)
              .getWorkspace();

      return ResponseEntity.ok(
          new AdminFederatedWorkspaceDetailsResponse()
              .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, firecloudWorkspace))
              .collaborators(collaborators)
              .resources(adminWorkspaceResources));
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }

  private WorkspaceUserAdminView toAdminView(UserRole userRole) {
    final Optional<DbUser> dbUserMaybe = userService.getByUsername(userRole.getEmail());
    final User user = new User();
    if (dbUserMaybe.isPresent()) {
      final DbUser dbUser = dbUserMaybe.get();
      user.email(dbUser.getUsername())
          .givenName(dbUser.getGivenName())
          .familyName(dbUser.getFamilyName())
          .email(dbUser.getContactEmail());
      return new WorkspaceUserAdminView()
          .userModel(user)
          .role(userRole.getRole())
          .userDatabaseId(dbUser.getUserId())
          .userAccountCreatedTime(DateTime.parse(dbUser.getCreationTime().toString()));

    } else {
      return new WorkspaceUserAdminView().userModel(userMapper.toUserApiModel(userRole, null));
    }
  }
}
