package org.pmiops.workbench.workspaceadmin;

import com.google.cloud.storage.BlobInfo;
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
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {
  private static final Duration TRAILING_TIME_TO_QUERY = Duration.ofHours(6);

  private final CloudStorageService cloudStorageService;
  private final CohortDao cohortDao;
  private final CloudMonitoringService cloudMonitoringService;
  private final ConceptSetDao conceptSetDao;
  private final DataSetDao dataSetDao;
  private final FirecloudMapper firecloudMapper;
  private final FireCloudService fireCloudService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final NotebooksService notebooksService;
  private final UserMapper userMapper;
  private final UserService userService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspaceAdminServiceImpl(
      CloudStorageService cloudStorageService,
      CohortDao cohortDao,
      CloudMonitoringService cloudMonitoringService,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      FireCloudService fireCloudService,
      FirecloudMapper firecloudMapper,
      LeonardoNotebooksClient leonardoNotebooksClient,
      NotebooksService notebooksService,
      UserMapper userMapper,
      UserService userService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService) {
    this.cloudStorageService = cloudStorageService;
    this.cohortDao = cohortDao;
    this.cloudMonitoringService = cloudMonitoringService;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.fireCloudService = fireCloudService;
    this.firecloudMapper = firecloudMapper;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.notebooksService = notebooksService;
    this.userMapper = userMapper;
    this.userService = userService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
  }

  @Override
  public Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace) {
    return workspaceDao.findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(workspaceNamespace);
  }

  @Override
  public AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId) {
    int cohortCount = cohortDao.countByWorkspaceId(workspaceId);
    int conceptSetCount = conceptSetDao.countByWorkspaceId(workspaceId);
    int dataSetCount = dataSetDao.countByWorkspaceId(workspaceId);
    return new AdminWorkspaceObjectsCounts()
        .cohortCount(cohortCount)
        .conceptSetCount(conceptSetCount)
        .datasetCount(dataSetCount);
  }

  @Override
  public AdminWorkspaceCloudStorageCounts getAdminWorkspaceCloudStorageCounts(
      String workspaceNamespace, String workspaceName) {
    String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    int notebookFilesCount = notebooksService.getNotebooksAsService(bucketName).size();
    int nonNotebookFilesCount = getNonNotebookFileCount(bucketName);
    long storageSizeBytes = getStorageSizeBytes(bucketName);

    return new AdminWorkspaceCloudStorageCounts()
        .notebookFileCount(notebookFilesCount)
        .nonNotebookFileCount(nonNotebookFilesCount)
        .storageBytesUsed(storageSizeBytes);
  }

  @Override
  public CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace) {
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

    return response;

  }

  @Override
  public AdminFederatedWorkspaceDetailsResponse getWorkspaceAdminView(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getFirstWorkspaceByNamespace(workspaceNamespace).orElseThrow(() -> new NotFoundException(""));

    final String workspaceFirecloudName = dbWorkspace.getFirecloudName();

    final List<WorkspaceUserAdminView> collaborators =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName)
            .stream()
            .map(this::toWorkspaceUserAdminView)
            .collect(Collectors.toList());

    final AdminWorkspaceObjectsCounts adminWorkspaceObjects = getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId());

    final AdminWorkspaceCloudStorageCounts adminWorkspaceCloudStorageCounts = getAdminWorkspaceCloudStorageCounts(
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

    return new AdminFederatedWorkspaceDetailsResponse()
            .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, firecloudWorkspace))
            .collaborators(collaborators)
            .resources(adminWorkspaceResources);
  }

  private int getNonNotebookFileCount(String bucketName) {
    return (int) cloudStorageService
        .getBlobListForPrefix(bucketName, NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY).stream()
        .filter(blob -> !NotebooksService.NOTEBOOK_PATTERN.matcher(blob.getName()).matches())
        .count();
  }

  private long getStorageSizeBytes(String bucketName) {
    return cloudStorageService.getBlobList(bucketName).stream()
        .map(BlobInfo::getSize)
        .reduce(0L, Long::sum);
  }

  // TODO(jaycarlton): move to appropriate mapper
  private WorkspaceUserAdminView toWorkspaceUserAdminView(UserRole userRole) {
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
