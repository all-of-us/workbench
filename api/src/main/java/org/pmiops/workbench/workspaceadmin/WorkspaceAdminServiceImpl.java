package org.pmiops.workbench.workspaceadmin;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.protobuf.util.Timestamps;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
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
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FireCloudService fireCloudService;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoApiClient leonardoNotebooksClient;
  private final LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  private final MailService mailService;
  private final NotebooksService notebooksService;
  private final Provider<DbUser> userProvider;
  private final UserMapper userMapper;
  private final UserService userService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;
  private final WorkspaceAuthService workspaceAuthService;

  @Autowired
  public WorkspaceAdminServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      AdminAuditor adminAuditor,
      CloudMonitoringService cloudMonitoringService,
      CloudStorageClient cloudStorageClient,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FireCloudService fireCloudService,
      LeonardoMapper leonardoMapper,
      LeonardoApiClient leonardoNotebooksClient,
      LeonardoRuntimeAuditor leonardoRuntimeAuditor,
      MailService mailService,
      NotebooksService notebooksService,
      Provider<WorkbenchConfig> workspaceConfigProvider,
      Provider<DbUser> userProvider,
      UserMapper userMapper,
      UserService userService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService,
      WorkspaceAuthService workspaceAuthService) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.adminAuditor = adminAuditor;
    this.cloudMonitoringService = cloudMonitoringService;
    this.cloudStorageClient = cloudStorageClient;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.fireCloudService = fireCloudService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.leonardoRuntimeAuditor = leonardoRuntimeAuditor;
    this.mailService = mailService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.userMapper = userMapper;
    this.userService = userService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
    this.workspaceAuthService = workspaceAuthService;
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
    int notebookFilesCount =
        notebooksService
            .getNotebooksAsService(bucketName, workspaceNamespace, workspaceName)
            .size();
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
        ? getActiveWorkspaceAdminView(dbWorkspace, workspaceNamespace)
        : getDeletedWorkspaceAdminView(dbWorkspace);
  }

  private WorkspaceAdminView getActiveWorkspaceAdminView(
      DbWorkspace dbWorkspace, String workspaceNamespace) {
    final String workspaceFirecloudName = dbWorkspace.getFirecloudName();

    final List<WorkspaceUserAdminView> collaborators =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName).stream()
            .map(this::toWorkspaceUserAdminView)
            .toList();

    final AdminWorkspaceCloudStorageCounts adminWorkspaceCloudStorageCounts =
        getAdminWorkspaceCloudStorageCounts(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    final AdminWorkspaceResources adminWorkspaceResources =
        new AdminWorkspaceResources()
            .workspaceObjects(getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId()))
            .cloudStorage(adminWorkspaceCloudStorageCounts);

    final RawlsWorkspaceDetails firecloudWorkspace =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceFirecloudName)
            .getWorkspace();

    Workspace workspace = workspaceMapper.toApiWorkspace(dbWorkspace, firecloudWorkspace);

    return new WorkspaceAdminView()
        .workspace(workspace)
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .collaborators(collaborators)
        .resources(adminWorkspaceResources)
        .activeStatus(dbWorkspace.getWorkspaceActiveStatusEnum());
  }

  private WorkspaceAdminView getDeletedWorkspaceAdminView(DbWorkspace dbWorkspace) {
    return new WorkspaceAdminView()
        .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, new RawlsWorkspaceDetails()))
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .activeStatus(dbWorkspace.getWorkspaceActiveStatusEnum());
  }

  @Override
  public List<ListRuntimeResponse> listRuntimes(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    return leonardoNotebooksClient
        .listRuntimesByProjectAsService(dbWorkspace.getGoogleProject())
        .stream()
        .map(leonardoMapper::toApiListRuntimeResponse)
        .toList();
  }

  @Override
  public List<UserAppEnvironment> listUserApps(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    return leonardoNotebooksClient.listAppsInProjectAsService(dbWorkspace.getGoogleProject());
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
                leonardoMapper.toGoogleProject(runtime.getCloudContext()),
                runtime.getRuntimeName()));
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
                        leonardoMapper.toGoogleProject(runtimeInBadState.getCloudContext()),
                        runtimeInBadState.getRuntimeName())));
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

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> listFiles(String workspaceNamespace, boolean onlyAppFiles) {
    final String workspaceName =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getFirecloudName();
    final String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();
    Set<String> workspaceUsers =
        workspaceAuthService.getFirecloudWorkspaceAcl(workspaceNamespace, workspaceName).keySet();
    // If onlyAppFiles is true get all Apps (Jupyter/Rmd/R) files, else return all files from bucket
    return onlyAppFiles
        ? notebooksService.getNotebooksAsService(bucketName, workspaceNamespace, workspaceName)
        : cloudStorageClient.getBlobPage(bucketName).stream()
            .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName, workspaceUsers))
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

    try {
      mailService.sendWorkspaceAdminLockingEmail(
          dbWorkspace,
          adminLockingRequest.getRequestReason(),
          workspaceService.getWorkspaceOwnerList(dbWorkspace));
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
  public DbWorkspace setPublished(
      String workspaceNamespace, String firecloudName, boolean publish) {
    final DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, firecloudName);

    fireCloudService.updateWorkspaceAclForPublishing(workspaceNamespace, firecloudName, publish);

    dbWorkspace.setPublished(publish);
    return workspaceDao.saveWithLastModified(dbWorkspace, userProvider.get());
  }

  @Override
  public void publishWorkspaceViaDB(
      String workspaceNamespace, PublishWorkspaceRequest publishWorkspaceRequest) {
    final DbWorkspace dbWorkspace =
        workspaceDao
            .getByNamespace(workspaceNamespace)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Workspace Namespace %s was not found", workspaceNamespace)));

    featuredWorkspaceDao
        .findByWorkspace(dbWorkspace)
        .ifPresentOrElse(
            (dbFeaturedWorkspace) -> {
              // Check if category in database is same as requested: If true do nothing, else update
              // database
              DbFeaturedCategory requestedCategory =
                  featuredWorkspaceMapper.toDbFeaturedCategory(
                      publishWorkspaceRequest.getCategory());

              DbFeaturedCategory existingCategory = dbFeaturedWorkspace.getCategory();
              if (existingCategory.equals(requestedCategory)) {
                log.warning(
                    String.format(
                        "Workspace %s is already published in the same category",
                        workspaceNamespace));
                return;
              }

              log.info(
                  String.format(
                      "Featured Workspace %s under category %s will be re-published by Admin under new category %s ",
                      workspaceNamespace, existingCategory, requestedCategory));
              DbFeaturedWorkspace dbFeaturedWorkspaceToUpdate =
                  featuredWorkspaceMapper.toDbFeaturedWorkspace(
                      dbFeaturedWorkspace, publishWorkspaceRequest);
              publishWorkspace(dbFeaturedWorkspaceToUpdate, existingCategory.toString());
            },
            () -> {
              // Update Acl in firecloud so that everyone can view the workspace
              fireCloudService.updateWorkspaceAclForPublishing(
                  dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), true);
              DbFeaturedWorkspace dbFeaturedWorkspaceToSave =
                  featuredWorkspaceMapper.toDbFeaturedWorkspace(
                      publishWorkspaceRequest, dbWorkspace);
              publishWorkspace(dbFeaturedWorkspaceToSave, null);
            });
  }

  private void publishWorkspace(
      DbFeaturedWorkspace dbFeaturedWorkspace, @Nullable String prevCategoryIfAny) {
    DbWorkspace dbWorkspace = dbFeaturedWorkspace.getWorkspace();

    FeaturedWorkspaceCategory requestedCategory =
        featuredWorkspaceMapper.toFeaturedWorkspaceCategory(dbFeaturedWorkspace.getCategory());

    // Save in database
    featuredWorkspaceDao.save(dbFeaturedWorkspace);

    // Fire Publish action type Audit action
    adminAuditor.firePublishWorkspaceAction(
        dbWorkspace.getWorkspaceId(), requestedCategory.toString(), prevCategoryIfAny);
    log.info(
        String.format(
            "Workspace %s has been published by Admin", dbWorkspace.getWorkspaceNamespace()));

    // Send Email to all workspace owners to let them know Workspace has been published
    sendEmailToWorkspaceOwners(dbWorkspace, true, requestedCategory);
  }

  @Override
  public void unpublishWorkspaceViaDB(String workspaceNamespace) {
    final DbWorkspace dbWorkspace =
        workspaceDao
            .getByNamespace(workspaceNamespace)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Workspace Namespace %s was not found", workspaceNamespace)));
    Optional<DbFeaturedWorkspace> dbFeaturedWorkspaceOptional =
        featuredWorkspaceDao.findByWorkspace(dbWorkspace);

    dbFeaturedWorkspaceOptional.ifPresentOrElse(
        (dbFeaturedWorkspace) -> {
          String featuredCategory = dbFeaturedWorkspace.getCategory().toString();

          fireCloudService.updateWorkspaceAclForPublishing(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), false);

          featuredWorkspaceDao.delete(dbFeaturedWorkspace);
          adminAuditor.fireUnpublishWorkspaceAction(dbWorkspace.getWorkspaceId(), featuredCategory);
          log.info(String.format("Workspace %s has been Unpublished by Admin", workspaceNamespace));

          // Send email to all workspace owners to let them know workspace has been unpublished
          sendEmailToWorkspaceOwners(dbWorkspace, false, null);
        },
        () -> {
          // If there is no entry in featuredWorkspace table i.e workspace has been unpublished do
          // nothing
          log.warning(String.format("Workspace %s is already Unpublished", workspaceNamespace));
        });
  }

  private void sendEmailToWorkspaceOwners(
      DbWorkspace dbWorkspace, boolean published, FeaturedWorkspaceCategory category) {
    final List<DbUser> owners = workspaceService.getWorkspaceOwnerList(dbWorkspace);
    try {
      if (published) {
        mailService.sendPublishWorkspaceEmail(dbWorkspace, owners, category);
      } else {
        mailService.sendUnpublishWorkspaceByAdminEmail(dbWorkspace, owners);
      }
    } catch (final MessagingException e) {
      log.log(Level.WARNING, e.getMessage());
    }
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  private int getNonNotebookFileCount(String bucketName) {
    return (int)
        cloudStorageClient.getBlobPage(bucketName).stream()
            .filter(((Predicate<Blob>) notebooksService::isManagedNotebookBlob).negate())
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
