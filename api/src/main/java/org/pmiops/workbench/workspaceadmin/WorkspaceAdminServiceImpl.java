package org.pmiops.workbench.workspaceadmin;

import com.google.common.collect.Streams;
import com.google.protobuf.util.Timestamps;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceRecoveryStatus;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.model.WorkspaceWaitingForRetrieval;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
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
  private final InitialCreditsService initialCreditsService;
  private final MailService mailService;
  private final NotebooksService notebooksService;
  private final UserMapper userMapper;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public WorkspaceAdminServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      AdminAuditor adminAuditor,
      CloudMonitoringService cloudMonitoringService,
      CloudStorageClient cloudStorageClient,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      InitialCreditsService initialCreditsService,
      MailService mailService,
      NotebooksService notebooksService,
      UserMapper userMapper,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.adminAuditor = adminAuditor;
    this.cloudMonitoringService = cloudMonitoringService;
    this.cloudStorageClient = cloudStorageClient;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.initialCreditsService = initialCreditsService;
    this.mailService = mailService;
    this.notebooksService = notebooksService;
    this.userMapper = userMapper;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
  public CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace) {
    String googleProject = getWorkspaceByNamespaceOrThrow(workspaceNamespace).getGoogleProject();

    return new CloudStorageTraffic()
        .receivedBytes(
            Streams.stream(
                    cloudMonitoringService
                        .getCloudStorageReceivedBytes(googleProject, TRAILING_TIME_TO_QUERY)
                        .iterator())
                .flatMap(timeSeries -> timeSeries.getPointsList().stream())
                .map(
                    point ->
                        new TimeSeriesPoint()
                            .timestamp(Timestamps.toMillis(point.getInterval().getEndTime()))
                            .value(point.getValue().getDoubleValue()))
                .sorted(Comparator.comparing(TimeSeriesPoint::getTimestamp))
                .toList());
  }

  @Override
  public WorkspaceAdminView getWorkspaceAdminView(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);

    return dbWorkspace.isActive()
        ? getActiveWorkspaceAdminView(dbWorkspace)
        : getDeletedWorkspaceAdminView(dbWorkspace);
  }

  private WorkspaceAdminView getActiveWorkspaceAdminView(DbWorkspace dbWorkspace) {

    final List<WorkspaceUserAdminView> collaborators =
        getWorkspaceCollaborators(dbWorkspace.getWorkspaceNamespace());

    Workspace workspace = workspaceMapper.toApiWorkspace(dbWorkspace, null, initialCreditsService);

    return new WorkspaceAdminView()
        .workspace(workspace)
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .collaborators(collaborators)
        .activeStatus(dbWorkspace.getWorkspaceActiveStatusEnum());
  }

  private WorkspaceAdminView getDeletedWorkspaceAdminView(DbWorkspace dbWorkspace) {
    return new WorkspaceAdminView()
        .workspace(
            workspaceMapper.toApiWorkspace(
                dbWorkspace, new RawlsWorkspaceDetails(), initialCreditsService))
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .activeStatus(dbWorkspace.getWorkspaceActiveStatusEnum());
  }

  private DbWorkspace getWorkspaceByNamespaceOrThrow(String workspaceNamespace) {
    return getFirstWorkspaceByNamespace(workspaceNamespace)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("No workspace found for namespace %s", workspaceNamespace)));
  }

  @Override
  public WorkspaceAuditLogQueryResponse getWorkspaceAuditLogEntries(
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
  public List<WorkspaceWaitingForRetrieval> getWorkspacesWaitingForRetrieval() {
    return workspaceDao
        .findAllByRecoveryState(WorkspaceRecoveryStatus.REQUESTED.toString())
        .stream()
        .map(
            dbWorkspace ->
                new WorkspaceWaitingForRetrieval()
                    .workspaceNamespace(dbWorkspace.getWorkspaceNamespace())
                    .workspaceName(dbWorkspace.getName())
                    .creator(
                        Optional.ofNullable(dbWorkspace.getCreator())
                            .map(DbUser::getUsername)
                            .orElse(null))
                    .lastModifiedTime(
                        Optional.ofNullable(dbWorkspace.getLastModifiedTime())
                            .map(ts -> ts.getTime())
                            .orElse(null)))
        .toList();
  }

  @Override
  public String getReadOnlyNotebook(
      String workspaceNamespace, String notebookNameWithFileExtension, AccessReason accessReason) {
    if (StringUtils.isBlank(accessReason.getReason())) {
      throw new BadRequestException("Notebook viewing access reason is required");
    }

    final String workspaceName =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getFirecloudName();
    adminAuditor.fireViewNotebookAction(
        workspaceNamespace, workspaceName, notebookNameWithFileExtension, accessReason);
    return notebooksService.adminGetReadOnlyHtml(
        workspaceNamespace, workspaceName, notebookNameWithFileExtension);
  }

  @Override
  public void updateBillingToCredits(String workspaceNamespace, String terraName) {
    try {
      DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, terraName);
      workspaceService.updateWorkspaceBillingAccount(
          dbWorkspace,
          workbenchConfigProvider.get().billing.initialCreditsBillingAccountName(),
          true);
    } catch (ServerErrorException e) {
      throw new ServerErrorException(
          "Could not update the billing account for " + workspaceNamespace, e);
    }
  }

  @Override
  public List<WorkspaceUserAdminView> getWorkspaceCollaborators(String namespace) {
    DbWorkspace workspace = workspaceDao.findByWorkspaceNamespace(namespace);
    List<ActionAuditQueryService.UserIdWithRoleImpl> collaborators =
        actionAuditQueryService.getWorkspaceUsersById(workspace.getWorkspaceId());
    return collaborators.stream()
        .map(
            c -> {
              WorkspaceUserAdminView adminUser = new WorkspaceUserAdminView();
              DbUser dbUser;
              try {
                dbUser = userDao.findUserByUserId(c.userId());
              } catch (Exception e) {
                log.info("Exception while fetching user: " + e.getMessage());
                return null;
              }
              if (dbUser == null) {
                return null;
              }
              adminUser.setUserModel(userMapper.toApiUser(dbUser));
              adminUser.setRole(WorkspaceAccessLevel.valueOf(c.role()));
              adminUser.setUserDatabaseId(dbUser.getUserId());
              return adminUser;
            })
        .filter(Objects::nonNull)
        .toList();
  }
}
