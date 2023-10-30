package org.pmiops.workbench.workspaces;

import bio.terra.workspace.model.*;
import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.transaction.Transactional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.model.CloudPlatform;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.tanagra.ApiException;
import org.pmiops.workbench.tanagra.model.Study;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.wsm.WsmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AwsWorkspaceServiceImpl implements AwsWorkspaceService, GaugeDataCollector {

  private static final Logger logger = LoggerFactory.getLogger(AwsWorkspaceServiceImpl.class);

  private final WsmClient wsmClient;

  private final WorkspaceDao workspaceDao;

  private final WorkspaceMapper workspaceMapper;

  private final Provider<DbUser> userProvider;

  @Autowired
  public AwsWorkspaceServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      Provider<DbUser> userProvider) {
    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.userProvider = userProvider;
  }

  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    logger.info("Getting AWS workspace with namespace: {}", workspaceNamespace);

    DbWorkspace dbWorkspace = workspaceDao.get(workspaceNamespace, workspaceId);
    if (dbWorkspace != null && isAws(dbWorkspace)) {
      WorkspaceDescription workspaceWithContext =
          wsmClient.getWorkspaceByNamespace(workspaceNamespace);

      String googleProjectId =
          (workspaceWithContext.getGcpContext() == null)
              ? null
              : workspaceWithContext.getGcpContext().getProjectId();

      logger.info(
          "Workspace context: userFacingId {}, project id: {}",
          workspaceWithContext.getUserFacingId(),
          googleProjectId);

      String awsS3StorageFolder = wsmClient.getAwsS3FolderName(workspaceWithContext.getId());

      WorkspaceResponse workspaceResponse = new WorkspaceResponse();

      // FIXME get from WSM
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.OWNER);

      workspaceResponse.setWorkspace(
          workspaceMapper.toApiWorkspace(
              dbWorkspace, getWorkspaceDetails(workspaceWithContext, awsS3StorageFolder)));

      return workspaceResponse;
    }

    return null;
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceId) {
    return false;
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    logger.info("Getting AWS workspaces");
    WorkspaceDescriptionList workspaceDescriptionList = null;
    try {
      workspaceDescriptionList = wsmClient.listWorkspaces();
    } catch (bio.terra.workspace.client.ApiException e) {
      logger.error("Error getting AWS workspaces", e);
      return Collections.emptyList();
    }

    logger.info("Got {} AWS workspaces", workspaceDescriptionList.getWorkspaces().size());

    return workspaceMapper
        .toApiWorkspaceResponses(
            workspaceDao,
            workspaceResponseListFromWorkspaceDescriptionList(workspaceDescriptionList))
        .stream()
        .map(workspaceResponse -> workspaceResponse.accessLevel(WorkspaceAccessLevel.OWNER))
        .filter(AwsWorkspaceServiceImpl::filterToNonPublished)
        .collect(Collectors.toList());
  }

  @Override
  public List<WorkspaceResponse> getPublishedWorkspaces() {
    return null;
  }

  @Override
  public String getPublishedWorkspacesGroupEmail() {
    return null;
  }

  @Override
  @Transactional
  public void deleteWorkspace(DbWorkspace dbWorkspace) {

    wsmClient.deleteWorkspace(dbWorkspace.getFirecloudUuid());

    workspaceDao.saveWithLastModified(
        dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED),
        userProvider.get());
  }

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {}

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return null;
  }

  @Override
  public Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> keySet) {
    return null;
  }

  @Override
  public DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace) {
    return null;
  }

  @Override
  public Study createTanagraStudy(String workspaceNamespace, String workspaceName)
      throws ApiException {
    return null;
  }

  @Override
  public RawlsWorkspaceDetails createWorkspace(Workspace workspace) {
    logger.info("Creating AWS workspace: {}", workspace.getNamespace());

    WorkspaceDescription workspaceDescription = wsmClient.createWorkspace(workspace);

    logger.info("AWS workspace created: {}", workspaceDescription.getId());
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (workspaceDescription.getAwsContext() != null) {
      logger.info("AWS context found, creating AWS bucket");
      CreatedControlledAwsS3StorageFolder awsBucket =
          wsmClient.createAwsFolder(workspace, workspaceDescription);

      // TODO Use MapStruct
      return getWorkspaceDetails(
          workspaceDescription, awsBucket.getAwsS3StorageFolder().getAttributes().getBucketName());
    }
    // FIXME throw here
    return null;
  }

  private RawlsWorkspaceDetails getWorkspaceDetails(
      WorkspaceDescription workspaceDescription, String awsBucketName) {
    RawlsWorkspaceDetails workspaceDetails = new RawlsWorkspaceDetails();
    workspaceDetails.setWorkspaceId(workspaceDescription.getId().toString());
    workspaceDetails.setName(workspaceDescription.getDisplayName());
    workspaceDetails.setNamespace(workspaceDescription.getUserFacingId());
    workspaceDetails.setCreatedBy(workspaceDescription.getCreatedBy());
    workspaceDetails.setCompletedCloneWorkspaceFileTransfer(OffsetDateTime.now());
    workspaceDetails.setBucketName(awsBucketName);
    workspaceDetails.setGoogleProject(workspaceDescription.getUserFacingId());
    return workspaceDetails;
  }

  private RawlsWorkspaceDetails getWorkspaceDetails(WorkspaceDescription workspaceDescription) {
    RawlsWorkspaceDetails workspaceDetails = new RawlsWorkspaceDetails();
    workspaceDetails.setWorkspaceId(workspaceDescription.getId().toString());
    workspaceDetails.setName(workspaceDescription.getDisplayName());
    workspaceDetails.setNamespace(workspaceDescription.getUserFacingId());
    workspaceDetails.setCreatedBy(workspaceDescription.getCreatedBy());
    workspaceDetails.setCompletedCloneWorkspaceFileTransfer(OffsetDateTime.now());
    return workspaceDetails;
  }

  private List<RawlsWorkspaceListResponse> workspaceResponseListFromWorkspaceDescriptionList(
      WorkspaceDescriptionList workspaceDescriptionList) {
    List<RawlsWorkspaceListResponse> responseList = new ArrayList<>();
    workspaceDescriptionList
        .getWorkspaces()
        .forEach(
            w -> {
              responseList.add(new RawlsWorkspaceListResponse().workspace(getWorkspaceDetails(w)));
            });
    return responseList;
  }

  private static boolean filterToNonPublished(WorkspaceResponse response) {
    return response.getAccessLevel() == WorkspaceAccessLevel.OWNER
        || response.getAccessLevel() == WorkspaceAccessLevel.WRITER
        || !response.getWorkspace().isPublished();
  }

  private boolean isAws(DbWorkspace workspace) {
    return workspace.getCloudPlatform().equals(CloudPlatform.AWS);
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    return workspaceDao.getWorkspaceCountGaugeData().stream()
        .map(
            row ->
                MeasurementBundle.builder()
                    .addMeasurement(GaugeMetric.WORKSPACE_COUNT, row.getWorkspaceCount())
                    .addTag(
                        MetricLabel.WORKSPACE_ACTIVE_STATUS, row.getActiveStatusEnum().toString())
                    .addTag(MetricLabel.ACCESS_TIER_SHORT_NAME, row.getTier().getShortName())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }
}
