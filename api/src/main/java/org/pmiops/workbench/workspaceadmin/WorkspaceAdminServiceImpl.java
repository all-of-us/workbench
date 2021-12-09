package org.pmiops.workbench.workspaceadmin;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {
  private static final Logger log = Logger.getLogger(WorkspaceAdminServiceImpl.class.getName());
  private static final Duration TRAILING_TIME_TO_QUERY = Duration.ofHours(6);

  private final ActionAuditQueryService actionAuditQueryService;
  private final AdminAuditor adminAuditor;
  private final CloudMonitoringService cloudMonitoringService;
  private final CloudStorageClient cloudStorageClient;
  private final CohortDao cohortDao;
  private final ConceptSetDao conceptSetDao;
  private final DataSetDao dataSetDao;
  private final FireCloudService fireCloudService;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  private final MailService mailService;
  private final NotebooksService notebooksService;
  private final UserMapper userMapper;
  private final UserService userService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspaceAdminServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      AdminAuditor adminAuditor,
      CloudMonitoringService cloudMonitoringService,
      CloudStorageClient cloudStorageClient,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      FireCloudService fireCloudService,
      LeonardoMapper leonardoMapper,
      LeonardoNotebooksClient leonardoNotebooksClient,
      LeonardoRuntimeAuditor leonardoRuntimeAuditor,
      MailService mailService,
      NotebooksService notebooksService,
      UserMapper userMapper,
      UserService userService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.adminAuditor = adminAuditor;
    this.cloudMonitoringService = cloudMonitoringService;
    this.cloudStorageClient = cloudStorageClient;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.fireCloudService = fireCloudService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.leonardoRuntimeAuditor = leonardoRuntimeAuditor;
    this.mailService = mailService;
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

    // NOTE: all of these may be undercounts, because we're only looking at the first Page of
    // Storage List results
    int notebookFilesCount = notebooksService.getNotebooksAsService(bucketName).size();
    int nonNotebookFilesCount = getNonNotebookFileCount(bucketName);
    long storageSizeBytes = getStorageSizeBytes(bucketName);

    return new AdminWorkspaceCloudStorageCounts()
        .notebookFileCount(notebookFilesCount)
        .nonNotebookFileCount(nonNotebookFilesCount)
        .storageBytesUsed(storageSizeBytes)
        .storageBucketPath(String.format("gs://%s", bucketName));
  }

  @Override
  public CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace) {
    CloudStorageTraffic response = new CloudStorageTraffic().receivedBytes(new ArrayList<>());
    String googleProject = getWorkspaceByNamespaceOrThrow(workspaceNamespace).getGoogleProject();
    for (TimeSeries timeSeries :
        cloudMonitoringService.getCloudStorageReceivedBytes(
            googleProject, TRAILING_TIME_TO_QUERY)) {
      for (Point point : timeSeries.getPointsList()) {
        response.addReceivedBytesItem(
            new TimeSeriesPoint()
                .timestamp(Timestamps.toMillis(point.getInterval().getEndTime()))
                .value(point.getValue().getDoubleValue()));
      }
    }

    response.getReceivedBytes().sort(Comparator.comparing(TimeSeriesPoint::getTimestamp));
    return response;
  }

  @Override
  public WorkspaceAdminView getWorkspaceAdminView(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);

    final String workspaceFirecloudName = dbWorkspace.getFirecloudName();

    final List<WorkspaceUserAdminView> collaborators =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName).stream()
            .map(this::toWorkspaceUserAdminView)
            .collect(Collectors.toList());

    final AdminWorkspaceObjectsCounts adminWorkspaceObjects =
        getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId());

    final AdminWorkspaceCloudStorageCounts adminWorkspaceCloudStorageCounts =
        getAdminWorkspaceCloudStorageCounts(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    final List<ListRuntimeResponse> workbenchListRuntimeResponses =
        leonardoNotebooksClient.listRuntimesByProjectAsService(dbWorkspace.getGoogleProject())
            .stream()
            .map(leonardoMapper::toApiListRuntimeResponse)
            .collect(Collectors.toList());

    final AdminWorkspaceResources adminWorkspaceResources =
        new AdminWorkspaceResources()
            .workspaceObjects(adminWorkspaceObjects)
            .cloudStorage(adminWorkspaceCloudStorageCounts)
            .runtimes(workbenchListRuntimeResponses);

    final FirecloudWorkspaceDetails firecloudWorkspace =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceFirecloudName)
            .getWorkspace();

    return new WorkspaceAdminView()
        .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, firecloudWorkspace))
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .collaborators(collaborators)
        .resources(adminWorkspaceResources);
  }

  @Override
  public List<ListRuntimeResponse> deleteRuntimesInWorkspace(
      String workspaceNamespace, ListRuntimeDeleteRequest req) {
    final String googleProject =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getGoogleProject();
    List<LeonardoListRuntimeResponse> runtimesToDelete =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(googleProject).stream(),
                req.getRuntimesToDelete())
            .collect(Collectors.toList());
    runtimesToDelete.forEach(
        runtime ->
            leonardoNotebooksClient.deleteRuntimeAsService(
                runtime.getGoogleProject(), runtime.getRuntimeName()));
    List<LeonardoListRuntimeResponse> runtimesInProjectAffected =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(googleProject).stream(),
                req.getRuntimesToDelete())
            .collect(Collectors.toList());
    // DELETED is an acceptable status from an implementation standpoint, but we will never
    // receive runtimes with that status from Leo. We don't want to because we reuse runtime
    // names and thus could have >1 deleted runtimes with the same name in the project.
    List<LeonardoRuntimeStatus> acceptableStates =
        ImmutableList.of(LeonardoRuntimeStatus.DELETING, LeonardoRuntimeStatus.ERROR);
    runtimesInProjectAffected.stream()
        .filter(runtime -> !acceptableStates.contains(runtime.getStatus()))
        .forEach(
            runtimeInBadState ->
                log.log(
                    Level.SEVERE,
                    String.format(
                        "Runtime %s/%s is not in a deleting state",
                        runtimeInBadState.getGoogleProject(), runtimeInBadState.getRuntimeName())));
    leonardoRuntimeAuditor.fireDeleteRuntimesInProject(
        googleProject,
        runtimesToDelete.stream()
            .map(LeonardoListRuntimeResponse::getRuntimeName)
            .collect(Collectors.toList()));
    return runtimesInProjectAffected.stream()
        .map(leonardoMapper::toApiListRuntimeResponse)
        .collect(Collectors.toList());
  }

  private DbWorkspace getWorkspaceByNamespaceOrThrow(String workspaceNamespace) {
    return getFirstWorkspaceByNamespace(workspaceNamespace)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("No workspace found for namespace %s", workspaceNamespace)));
  }

  @Override
  public WorkspaceAuditLogQueryResponse getAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable) {
    final long workspaceDatabaseId =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getWorkspaceId();
    final Instant after = Instant.ofEpochMilli(afterMillis);
    final Instant before =
        Optional.ofNullable(beforeMillisNullable).map(Instant::ofEpochMilli).orElse(Instant.now());
    return actionAuditQueryService.queryEventsForWorkspace(
        workspaceDatabaseId, limit, after, before);
  }

  @Override
  public String getReadOnlyNotebook(
      String workspaceNamespace, String notebookName, AccessReason accessReason) {
    if (StringUtils.isBlank(accessReason.getReason())) {
      throw new BadRequestException("Notebook viewing access reason is required");
    }

    final String workspaceName =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getFirecloudName();
    adminAuditor.fireViewNotebookAction(
        workspaceNamespace, workspaceName, notebookName, accessReason);
    return notebooksService.adminGetReadOnlyHtml(workspaceNamespace, workspaceName, notebookName);
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> listFiles(String workspaceNamespace) {
    final String workspaceName =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getFirecloudName();
    final String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    return cloudStorageClient.getBlobPage(bucketName).stream()
        .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName))
        .collect(Collectors.toList());
  }

  @Override
  public void setAdminLockedState(
      String workspaceNamespace, AdminLockingRequest adminLockingRequest) {
    log.info(String.format("called setAdminLockedState on wsns %s", workspaceNamespace));

    DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    dbWorkspace.setAdminLocked(true).setAdminLockedReason(adminLockingRequest.getRequestReason());
    dbWorkspace = workspaceDao.save(dbWorkspace);
    adminAuditor.fireLockWorkspaceAction(dbWorkspace.getWorkspaceId(), adminLockingRequest);

    final List<DbUser> owners =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName())
            .stream()
            .filter(userRole -> userRole.getRole() == WorkspaceAccessLevel.OWNER)
            .map(UserRole::getEmail)
            .map(userService::getByUsernameOrThrow)
            .collect(Collectors.toList());
    try {
      mailService.sendWorkspaceAdminLockingEmail(
          dbWorkspace, adminLockingRequest.getRequestReason(), owners);
    } catch (final MessagingException e) {
      log.log(Level.WARNING, e.getMessage());
    }
  }

  @Override
  public void setAdminUnlockedState(String workspaceNamespace) {
    log.info(String.format("called setAdminUnlockedState on wsns %s", workspaceNamespace));

    DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    workspaceDao.save(dbWorkspace.setAdminLocked(false));
    adminAuditor.fireUnlockWorkspaceAction(dbWorkspace.getWorkspaceId());
  }

  @Override
  public void setResearchPurposeApproved(
      String workspaceNamespace, String firecloudName, boolean approved) {
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, firecloudName);
    if (workspace.getReviewRequested() == null || !workspace.getReviewRequested()) {
      throw new BadRequestException(
          String.format(
              "No review requested for workspace %s/%s.", workspaceNamespace, firecloudName));
    }

    Boolean existingApproval = workspace.getApproved();
    if (existingApproval != null) {
      throw new BadRequestException(
          String.format(
              "DbWorkspace %s/%s already %s.",
              workspaceNamespace, firecloudName, existingApproval ? "approved" : "rejected"));
    }
    workspace.setApproved(approved);
    workspaceDao.saveWithLastModified(workspace);

    // RW-7087 replace with a new workspaceAuditor action (fireReviewAction?)
    // because this uses the deprecated DbAdminActionHistory
    userService.logAdminWorkspaceAction(
        workspace.getWorkspaceId(),
        "research purpose approval",
        workspace.getApproved(),
        existingApproval);
  }

  @Override
  public List<Workspace> getWorkspacesForReview() {
    return workspaceDao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested().stream()
        .map(workspaceMapper::toApiWorkspace)
        .collect(Collectors.toList());
  }

  @Override
  public DbWorkspace setPublished(
      String workspaceNamespace, String firecloudName, boolean publish) {
    final DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, firecloudName);

    final WorkspaceAccessLevel accessLevel =
        publish ? WorkspaceAccessLevel.READER : WorkspaceAccessLevel.NO_ACCESS;

    final FirecloudManagedGroupWithMembers authDomainGroup =
        fireCloudService.getGroup(dbWorkspace.getCdrVersion().getAccessTier().getAuthDomainName());

    final FirecloudWorkspaceACLUpdate currentUpdate =
        WorkspaceAuthService.updateFirecloudAclsOnUser(
            accessLevel, new FirecloudWorkspaceACLUpdate().email(authDomainGroup.getGroupEmail()));

    fireCloudService.updateWorkspaceACL(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        Collections.singletonList(currentUpdate));

    dbWorkspace.setPublished(publish);
    return workspaceDao.saveWithLastModified(dbWorkspace);
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  private int getNonNotebookFileCount(String bucketName) {
    return (int)
        cloudStorageClient
            .getBlobPageForPrefix(bucketName, NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY)
            .stream()
            .filter(((Predicate<Blob>) notebooksService::isNotebookBlob).negate())
            .count();
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  private long getStorageSizeBytes(String bucketName) {
    return cloudStorageClient.getBlobPage(bucketName).stream()
        .map(BlobInfo::getSize)
        .reduce(0L, Long::sum);
  }

  // This is somewhat awkward, as we want to tolerate collaborators who aren't in the database
  // anymore.
  // TODO(jaycarlton): is this really what we want, or can we make this return an Optional that's
  // empty
  // when the user isn't in the DB. The assumption is that the fields agree between the UserRole and
  // the DbUser, but we don't check that here.
  private WorkspaceUserAdminView toWorkspaceUserAdminView(UserRole userRole) {
    return userService
        .getByUsername(userRole.getEmail())
        .map(u -> userMapper.toWorkspaceUserAdminView(u, userRole))
        .orElse(
            new WorkspaceUserAdminView() // the MapStruct-generated method won't handle a partial
                // conversion
                .role(userRole.getRole())
                .userModel(userMapper.toApiUser(userRole, null)));
  }

  private static Stream<LeonardoListRuntimeResponse> filterByRuntimesInList(
      Stream<LeonardoListRuntimeResponse> runtimesToFilter, List<String> runtimeNames) {
    // Null means keep all runtimes.
    return runtimesToFilter.filter(
        runtime -> runtimeNames == null || runtimeNames.contains(runtime.getRuntimeName()));
  }
}
