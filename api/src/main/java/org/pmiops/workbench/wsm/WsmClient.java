package org.pmiops.workbench.wsm;

import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.*;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** A Wrapper for WSM APIs */
@Service
public class WsmClient {

  private static final Logger logger = LoggerFactory.getLogger(WsmClient.class);

  private final Provider<WorkspaceApi> workspaceApi;
  private final Provider<ResourceApi> resourceApiProvider;

  private final Provider<ControlledAwsResourceApi> awsResourceApiProvider;

  private final WsmRetryHandler wsmRetryHandler;

  public WsmClient(
      Provider<WorkspaceApi> workspaceApi,
      Provider<ResourceApi> resourceApiProvider,
      Provider<ControlledAwsResourceApi> awsResourceApiProvider,
      WsmRetryHandler wsmRetryHandler) {
    this.workspaceApi = workspaceApi;
    this.resourceApiProvider = resourceApiProvider;
    this.awsResourceApiProvider = awsResourceApiProvider;
    this.wsmRetryHandler = wsmRetryHandler;
  }

  // ---------------- Workspaces ----------------
  /**
   * Creates a new workspace and returns its description.
   *
   * @param workspace The workspace to be created.
   * @return The description of the created workspace.
   */
  public WorkspaceDescription createWorkspace(Workspace workspace) {
    return wsmRetryHandler.run(
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
                  .cloudPlatform(bio.terra.workspace.model.CloudPlatform.AWS)
                  .jobControl(new JobControl().id(UUID.randomUUID().toString()));

          workspaceApi.get().createWorkspaceV2(workspaceV2Request);

          // call the get workspace endpoint to get the full description object
          return workspaceApi.get().getWorkspace(workspaceId, null);
        });
  }

  /**
   * Retrieves the workspace description based on the provided workspace namespace.
   *
   * @param workspaceNamespace The namespace of the workspace.
   * @return The workspace description corresponding to the provided namespace.
   */
  public WorkspaceDescription getWorkspaceByNamespace(String workspaceNamespace) {
    return wsmRetryHandler.run(
        context ->
            workspaceApi
                .get()
                .getWorkspaceByUserFacingId(workspaceNamespace, /*minimumHighestRole=*/ null));
  }

  /**
   * Lists all workspaces and returns the descriptions of the listed workspaces.
   *
   * @return A list containing the descriptions of all the listed workspaces.
   * @throws ApiException if there is an error while listing the workspaces.
   */
  public WorkspaceDescriptionList listWorkspaces() throws ApiException {
    return workspaceApi.get().listWorkspaces(0, 30, /*minimumHighestRole=*/ null);
  }

  /**
   * Deletes the workspace identified by the provided workspace UUID.
   *
   * @param workspaceUUID The UUID of the workspace to be deleted.
   */
  public void deleteWorkspace(String workspaceUUID) {
    wsmRetryHandler.run(
        context ->
            workspaceApi
                .get()
                .deleteWorkspaceV2(
                    new DeleteWorkspaceV2Request()
                        .jobControl(new JobControl().id(UUID.randomUUID().toString())),
                    UUID.fromString(workspaceUUID)));
    logger.info("AWS  Workspace deleted: {}", workspaceUUID);
  }

  // ---------------- Storage folders ----------------

  /**
   * Creates an AWS folder within the specified workspace and returns the created controlled AWS S3
   * storage folder.
   *
   * @param workspace The workspace in which the AWS folder will be created.
   * @param workspaceDescription The description of the workspace where the AWS folder will be
   *     created.
   * @return The created controlled AWS S3 storage folder.
   */
  public CreatedControlledAwsS3StorageFolder createAwsFolder(
      Workspace workspace, WorkspaceDescription workspaceDescription) {

    return wsmRetryHandler.run(
        context -> {
          CreateControlledAwsS3StorageFolderRequestBody body =
              new CreateControlledAwsS3StorageFolderRequestBody()
                  .awsS3StorageFolder(
                      new AwsS3StorageFolderCreationParameters().region("us-east-1"))
                  .common(
                      createCommonFields(
                          workspace.getName().replaceAll("\\s", "-"),
                          workspace.getNamespace(),
                          AccessScope.PRIVATE_ACCESS,
                          CloningInstructionsEnum.RESOURCE));
          return awsResourceApiProvider
              .get()
              .createAwsS3StorageFolder(body, workspaceDescription.getId());
        });
  }

  /**
   * Retrieves the AWS S3 storage folder name for the provided UUID.
   *
   * @param workspaceUUID The UUID of the workspace.
   * @return The name of the AWS S3 storage folder corresponding to the provided UUID.
   */
  public String getAwsS3FolderName(UUID workspaceUUID) {
    ResourceList resourceList;
    try {
      resourceList = getResourcesInWorkspace(workspaceUUID, 10, ResourceType.AWS_S3_STORAGE_FOLDER);
    } catch (bio.terra.workspace.client.ApiException e) {
      throw new WorkbenchException(e);
    }

    return resourceList
        .getResources()
        .get(0)
        .getResourceAttributes()
        .getAwsS3StorageFolder()
        .getBucketName();
  }

  /**
   * Retrieves the AWS S3 storage folder for the provided UUID.
   *
   * @param workspaceUUID The UUID of the workspace.
   * @return The AWS S3 storage folder corresponding to the provided UUID.
   */
  public ResourceDescription getAwsS3Folder(UUID workspaceUUID) {
    ResourceList resourceList;
    try {
      resourceList = getResourcesInWorkspace(workspaceUUID, 10, ResourceType.AWS_S3_STORAGE_FOLDER);
    } catch (bio.terra.workspace.client.ApiException e) {
      throw new WorkbenchException(e);
    }

    return resourceList.getResources().get(0);
  }

  public AwsCredential getAwsS3Credential(String workspaceUUID, UUID folderResourceUUID)
      throws ApiException {
    return awsResourceApiProvider
        .get()
        .getAwsS3StorageFolderCredential(
            UUID.fromString(workspaceUUID),
            folderResourceUUID,
            AwsCredentialAccessScope.READ_ONLY,
            900);
  }

  // ---------------- General Resources ----------------

  /**
   * Retrieves the list of resources in the workspace with retries.
   *
   * @param workspaceUUID The UUID of the workspace.
   * @param resourceType The type of the resource.
   * @return The list of resources in the workspace.
   */
  public ResourceList getResourcesInWorkspaceWithRetries(
      String workspaceUUID, ResourceType resourceType) {
    return wsmRetryHandler.run(
        context -> getResourcesInWorkspace(UUID.fromString(workspaceUUID), 20, resourceType));
  }

  public ResourceList getResourcesInWorkspace(
      UUID workspaceUUID, int limit, ResourceType resourceType) throws ApiException {
    return resourceApiProvider
        .get()
        .enumerateResources(workspaceUUID, 0, limit, resourceType, StewardshipType.CONTROLLED);
  }

  public ResourceList getResourcesInWorkspace(
      String workspaceUUID, int limit, ResourceType resourceType) throws ApiException {
    return getResourcesInWorkspace(UUID.fromString(workspaceUUID), limit, resourceType);
  }

  // ---------------- Notebooks ----------------

  /**
   * Retrieves an optional Sagemaker notebook resource based on the provided application name and
   * workspace UUID.
   *
   * @param appName The name of the application.
   * @param workspaceUUID The UUID of the workspace.
   * @return An optional Sagemaker notebook resource.
   */
  public Optional<ResourceDescription> getSagemakerNotebookMaybe(
      String appName, String workspaceUUID) {
    return wsmRetryHandler.run(
        context -> {
          ResourceList resourceList =
              getResourcesInWorkspace(
                  UUID.fromString(workspaceUUID), 10, ResourceType.AWS_SAGEMAKER_NOTEBOOK);
          return resourceList.getResources().stream()
              .filter(resource -> resource.getMetadata().getName().equals(appName))
              .findFirst();
        });
  }

  /**
   * Creates a Sagemaker notebook asynchronously.
   *
   * @param workspaceUUID The UUID of the workspace where the Sagemaker notebook will be created.
   * @param name The name of the Sagemaker notebook.
   * @param workspaceNamespace The namespace of the workspace.
   */
  public void createSageMakerNotebookAsync(
      String workspaceUUID, String name, String workspaceNamespace) {

    CreateControlledAwsSageMakerNotebookResult sageMakerNotebookResult =
        wsmRetryHandler.run(
            context -> {
              String validName = name.trim().replaceAll("\\s", "-");

              CreateControlledAwsSageMakerNotebookRequestBody body =
                  new CreateControlledAwsSageMakerNotebookRequestBody()
                      .awsSageMakerNotebook(
                          new AwsSageMakerNotebookCreationParameters()
                              .instanceName("notebook-" + validName)
                              .region("us-east-1")
                              .instanceType("ml.t2.medium"))
                      .common(
                          createCommonFields(
                              validName + "-nb",
                              workspaceNamespace,
                              AccessScope.PRIVATE_ACCESS,
                              CloningInstructionsEnum.NOTHING));
              body.setJobControl(new JobControl().id(UUID.randomUUID().toString()));
              return awsResourceApiProvider
                  .get()
                  .createAwsSageMakerNotebook(body, UUID.fromString(workspaceUUID));
            });

    logger.info(
        "Creating sagemaker notebook with job ID: {} , url is: {}",
        sageMakerNotebookResult.getJobReport().getId(),
        sageMakerNotebookResult.getJobReport().getResultURL());
  }

  /**
   * Deletes the sagemaker notebook identified by the provided name and workspace UUID.
   *
   * @param notebookName The name of the application to be deleted.
   * @param workspaceUUID The UUID of the workspace.
   */
  public void deleteSagemakerNotebook(String notebookName, String workspaceUUID) {
    Optional<ResourceDescription> notebookMaybe =
        this.getSagemakerNotebookMaybe(notebookName, workspaceUUID);

    if (!notebookMaybe.isPresent()) {
      return;
    }

    final UUID resourceId = notebookMaybe.get().getMetadata().getResourceId();
    wsmRetryHandler.run(
        context ->
            awsResourceApiProvider
                .get()
                .deleteAwsSageMakerNotebook(
                    new DeleteControlledAwsResourceRequestBody()
                        .jobControl(new JobControl().id(UUID.randomUUID().toString())),
                    UUID.fromString(workspaceUUID),
                    resourceId));
  }

  /**
   * Retrieves the AWS Sagemaker notebookResourceUUID credential based on the provided workspace
   * UUID and notebookResourceUUID description.
   *
   * @param workspaceUUID The UUID of the workspace.
   * @param notebookResourceUUID The description of the notebookResourceUUID.
   * @return The AWS Sagemaker notebookResourceUUID credential.
   * @throws ApiException if there is an error while retrieving the credential.
   */
  public AwsCredential getAwsSageMakerNotebookCredential(
      String workspaceUUID, UUID notebookResourceUUID) throws ApiException {
    return awsResourceApiProvider
        .get()
        .getAwsSageMakerNotebookCredential(
            UUID.fromString(workspaceUUID),
            notebookResourceUUID,
            AwsCredentialAccessScope.READ_ONLY,
            900);
  }

  /*-----------------*/
  /* Private methods */
  /*-----------------*/

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
}
