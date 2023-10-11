package org.pmiops.workbench.workspaces;

import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.model.*;
import bio.terra.workspace.model.Properties;
import bio.terra.workspace.model.ResourceType;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.transaction.Transactional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.tanagra.ApiException;
import org.pmiops.workbench.tanagra.model.Study;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.wsm.WsmRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AwsWorkspaceServiceImpl implements AwsWorkspaceService {

  private static final Logger logger = LoggerFactory.getLogger(AwsWorkspaceServiceImpl.class);

  private final Provider<WorkspaceApi> workspaceApi;
  private final Provider<ResourceApi> resourceApiProvider;

  private final Provider<ControlledAwsResourceApi> awsResourceApiProvider;

  private final WsmRetryHandler wsmRetryHandler;

  private final WorkspaceDao workspaceDao;

  private final WorkspaceMapper workspaceMapper;

  private final Provider<DbUser> userProvider;

  @Autowired
  public AwsWorkspaceServiceImpl(
      Provider<WorkspaceApi> workspaceApi,
      Provider<ResourceApi> resourceApiProvider,
      Provider<ControlledAwsResourceApi> awsResourceApiProvider,
      WsmRetryHandler wsmRetryHandler,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      Provider<DbUser> userProvider) {
    this.workspaceApi = workspaceApi;
    this.resourceApiProvider = resourceApiProvider;
    this.awsResourceApiProvider = awsResourceApiProvider;
    this.wsmRetryHandler = wsmRetryHandler;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.userProvider = userProvider;
  }

  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.get(workspaceNamespace, workspaceId);
    if (dbWorkspace != null && dbWorkspace.isAws()) {
      WorkspaceDescription workspaceWithContext =
          wsmRetryHandler.run(
              context ->
                  workspaceApi
                      .get()
                      .getWorkspaceByUserFacingId(
                          workspaceNamespace, /*minimumHighestRole=*/ null));

      String googleProjectId =
          (workspaceWithContext.getGcpContext() == null)
              ? null
              : workspaceWithContext.getGcpContext().getProjectId();
      logger.debug(
          "Workspace context: userFacingId {}, project id: {}",
          workspaceWithContext.getUserFacingId(),
          googleProjectId);

      ResourceList resourceList;
      try {
        resourceList =
            resourceApiProvider
                .get()
                .enumerateResources(
                    workspaceWithContext.getId(),
                    0,
                    10,
                    ResourceType.AWS_S3_STORAGE_FOLDER,
                    StewardshipType.CONTROLLED);
      } catch (bio.terra.workspace.client.ApiException e) {
        throw new WorkbenchException(e);
      }

      String awsS3StorageFolder =
          resourceList
              .getResources()
              .get(0)
              .getResourceAttributes()
              .getAwsS3StorageFolder()
              .getBucketName();

      WorkspaceResponse workspaceResponse = new WorkspaceResponse();

      // FIXME get from WSM
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.OWNER);

      workspaceResponse.setWorkspace(
          workspaceMapper.toApiWorkspace(
              dbWorkspace, getWorkspaceDetails(workspaceWithContext, awsS3StorageFolder)));

      return workspaceResponse;
    }
    // validateWorkspaceTierAccess(dbWorkspace);
    return null;
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceId) {
    return false;
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    WorkspaceDescriptionList workspaceDescriptionList =
        wsmRetryHandler.run(
            context -> workspaceApi.get().listWorkspaces(0, 30, /*minimumHighestRole=*/ null));

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
    wsmRetryHandler.run(
        context ->
            workspaceApi
                .get()
                .deleteWorkspaceV2(
                    new DeleteWorkspaceV2Request()
                        .jobControl(new JobControl().id(UUID.randomUUID().toString())),
                    UUID.fromString(dbWorkspace.getFirecloudUuid())));

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

    WorkspaceDescription workspaceDescription =
        wsmRetryHandler.run(
            context -> {
              UUID workspaceId = UUID.randomUUID();
              // logger.info("Workspace creation in progress, id: {}", workspaceId);
              CreateWorkspaceV2Request workspaceV2Request =
                  new CreateWorkspaceV2Request()
                      .id(workspaceId)
                      .userFacingId(workspace.getNamespace())
                      .displayName(workspace.getName())
                      .description(workspace.getNamespace())
                      .stage(WorkspaceStageModel.MC_WORKSPACE)
                      .properties(stringMapToProperties(Collections.emptyMap()))
                      .spendProfile("wm-default-spend-profile")
                      .cloudPlatform(CloudPlatform.AWS)
                      .jobControl(new JobControl().id(UUID.randomUUID().toString()));

              workspaceApi.get().createWorkspaceV2(workspaceV2Request);

              // call the get workspace endpoint to get the full description object
              return workspaceApi.get().getWorkspace(workspaceId, null);
            });

    // TODO, this should also accept the cloud context, depending on what gets passed from the
    // caller
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (workspaceDescription.getAwsContext() != null) {
      CreatedControlledAwsS3StorageFolder awsBucket =
          createAwsBucket(workspace, workspaceDescription);

      // logger.info("Workspace created, id: {}", workspaceDescription.getId());
      // Map workspaceDescription to WorkspaceDetails
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
    // workspaceDetails.setBucketName(awsBucket.getMetadata().getName());
    return workspaceDetails;
  }

  private CreatedControlledAwsS3StorageFolder createAwsBucket(
      Workspace workspace, WorkspaceDescription workspaceDescription) {

    return wsmRetryHandler.run(
        context -> {
          CreateControlledAwsS3StorageFolderRequestBody body =
              new CreateControlledAwsS3StorageFolderRequestBody()
                  .awsS3StorageFolder(
                      new AwsS3StorageFolderCreationParameters().region("us-east-1"))
                  .common(
                      createCommonFields(
                          workspace.getName(),
                          workspace.getNamespace(),
                          AccessScope.SHARED_ACCESS,
                          CloningInstructionsEnum.RESOURCE));
          return awsResourceApiProvider
              .get()
              .createAwsS3StorageFolder(body, workspaceDescription.getId());
        });
  }

  private ControlledResourceCommonFields createCommonFields(
      String name,
      String description,
      AccessScope accessScope,
      CloningInstructionsEnum cloningInstructions) {
    return new ControlledResourceCommonFields()
        .name(name)
        .description(description)
        .cloningInstructions(cloningInstructions)
        .accessScope(accessScope)
        .managedBy(ManagedBy.USER);
  }

  private static Properties stringMapToProperties(Map<String, String> map) {
    Properties properties = new Properties();
    if (map == null) {
      return properties;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      Property property = new Property().key(entry.getKey()).value(entry.getValue());
      properties.add(property);
    }
    return properties;
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
}
