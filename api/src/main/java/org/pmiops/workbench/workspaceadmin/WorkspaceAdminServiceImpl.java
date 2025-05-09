package org.pmiops.workbench.workspaceadmin;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.common.collect.Streams;
import com.google.protobuf.util.Timestamps;
import jakarta.annotation.Nullable;
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
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
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
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminRuntimeFields;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
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
  private final InitialCreditsService initialCreditsService;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoApiClient leonardoApiClient;
  private final LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  private final MailService mailService;
  private final NotebooksService notebooksService;
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
      InitialCreditsService initialCreditsService,
      LeonardoMapper leonardoMapper,
      LeonardoApiClient leonardoApiClient,
      LeonardoRuntimeAuditor leonardoRuntimeAuditor,
      MailService mailService,
      NotebooksService notebooksService,
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
    this.initialCreditsService = initialCreditsService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoApiClient = leonardoApiClient;
    this.leonardoRuntimeAuditor = leonardoRuntimeAuditor;
    this.mailService = mailService;
    this.notebooksService = notebooksService;
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
      String workspaceNamespace, String workspaceTerraName) {
    String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();

    // NOTE: all of these may be undercounts, because we're only looking at the first Page of
    // Storage List results
    int notebookFilesCount =
        notebooksService
            .getNotebooksAsService(bucketName, workspaceNamespace, workspaceTerraName)
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

    var userRoles =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName);
    var userMap =
        userService.getUsersMappedByUsernames(userRoles.stream().map(UserRole::getEmail).toList());

    final List<WorkspaceUserAdminView> collaborators =
        userRoles.stream()
            .map(ur -> toWorkspaceUserAdminView(ur, userMap.get(ur.getEmail())))
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

    Workspace workspace =
        workspaceMapper.toApiWorkspace(dbWorkspace, firecloudWorkspace, initialCreditsService);

    return new WorkspaceAdminView()
        .workspace(workspace)
        .workspaceDatabaseId(dbWorkspace.getWorkspaceId())
        .collaborators(collaborators)
        .resources(adminWorkspaceResources)
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

  @Override
  public List<AdminRuntimeFields> listRuntimes(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    return leonardoApiClient.listRuntimesByProjectAsService(dbWorkspace.getGoogleProject()).stream()
        .map(leonardoMapper::toAdminRuntimeFields)
        .toList();
  }

  @Override
  public List<UserAppEnvironment> listUserApps(String workspaceNamespace) {
    final DbWorkspace dbWorkspace = getWorkspaceByNamespaceOrThrow(workspaceNamespace);
    return leonardoApiClient.listAppsInProjectAsService(dbWorkspace.getGoogleProject());
  }

  @Override
  public AdminRuntimeFields deleteRuntime(String workspaceNamespace, String runtimeNameToDelete) {
    final String googleProject =
        getWorkspaceByNamespaceOrThrow(workspaceNamespace).getGoogleProject();
    leonardoApiClient.deleteRuntimeAsService(
        googleProject, runtimeNameToDelete, /* deleteDisk */ false);

    // fetch again to confirm deletion
    LeonardoGetRuntimeResponse refreshedRuntime =
        leonardoApiClient.getRuntimeAsService(googleProject, runtimeNameToDelete);

    // DELETED is an acceptable status from an implementation standpoint, but we will never
    // receive runtimes with that status from Leo. We don't want to because we reuse runtime
    // names and thus could have >1 deleted runtimes with the same name in the project.
    List<LeonardoRuntimeStatus> acceptableStates =
        List.of(LeonardoRuntimeStatus.DELETING, LeonardoRuntimeStatus.ERROR);
    if (!acceptableStates.contains(refreshedRuntime.getStatus())) {
      log.log(
          Level.SEVERE,
          String.format(
              "Runtime %s/%s is not in a deleting state", googleProject, runtimeNameToDelete));
    }

    leonardoRuntimeAuditor.fireDeleteRuntime(googleProject, runtimeNameToDelete);
    return leonardoMapper.toAdminRuntimeFields(refreshedRuntime);
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

    DbWorkspace dbWorkspace =
        workspaceDao.save(
            getWorkspaceByNamespaceOrThrow(workspaceNamespace)
                .setAdminLocked(true)
                .setAdminLockedReason(adminLockingRequest.getRequestReason()));
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

    DbWorkspace dbWorkspace =
        workspaceDao.save(getWorkspaceByNamespaceOrThrow(workspaceNamespace).setAdminLocked(false));
    adminAuditor.fireUnlockWorkspaceAction(dbWorkspace.getWorkspaceId());
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
            dbFeaturedWorkspace -> {
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

    // send an email to all workspace owners to let them know that the workspace has been
    // published, but only if it's a newly published Community Workspace

    if (requestedCategory.equals(FeaturedWorkspaceCategory.COMMUNITY)
        && prevCategoryIfAny == null) {
      try {
        mailService.sendPublishCommunityWorkspaceEmails(
            dbWorkspace, workspaceService.getWorkspaceOwnerList(dbWorkspace));
      } catch (final MessagingException e) {
        log.log(Level.WARNING, e.getMessage());
      }
    }
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
        dbFeaturedWorkspace -> {
          String featuredCategory = dbFeaturedWorkspace.getCategory().toString();

          fireCloudService.updateWorkspaceAclForPublishing(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), false);

          featuredWorkspaceDao.delete(dbFeaturedWorkspace);
          adminAuditor.fireUnpublishWorkspaceAction(dbWorkspace.getWorkspaceId(), featuredCategory);
          log.info(String.format("Workspace %s has been Unpublished by Admin", workspaceNamespace));

          // Send email to all workspace owners to let them know workspace has been unpublished
          try {
            mailService.sendAdminUnpublishWorkspaceEmails(
                dbWorkspace, workspaceService.getWorkspaceOwnerList(dbWorkspace));
          } catch (final MessagingException e) {
            log.log(Level.WARNING, e.getMessage());
          }
        },
        () ->
            // If there is no entry in featuredWorkspace table i.e workspace has been unpublished do
            // nothing
            log.warning(String.format("Workspace %s is already Unpublished", workspaceNamespace)));
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
  private WorkspaceUserAdminView toWorkspaceUserAdminView(
      UserRole userRole, @Nullable DbUser userMaybe) {
    return userMaybe == null
        ?
        // the MapStruct-generated method won't handle a partial conversion
        new WorkspaceUserAdminView()
            .role(userRole.getRole())
            .userModel(userMapper.toApiUser(userRole, null))
        : userMapper.toWorkspaceUserAdminView(userMaybe, userRole);
  }
}
