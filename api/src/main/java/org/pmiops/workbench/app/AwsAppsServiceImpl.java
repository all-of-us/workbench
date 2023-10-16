package org.pmiops.workbench.app;

import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.*;
import java.net.URL;
import java.util.*;
import javax.inject.Provider;
import org.pmiops.workbench.aws.sagemaker.AwsSagemakerService;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.utils.mappers.WsmMapper;
import org.pmiops.workbench.wsm.WsmRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

@Service("awsAppsService")
public class AwsAppsServiceImpl implements AppsService {

  private static final Logger logger = LoggerFactory.getLogger(AwsAppsServiceImpl.class);

  private final WsmRetryHandler wsmRetryHandler;

  private final Provider<ResourceApi> resourceApiProvider;
  private final Provider<ControlledAwsResourceApi> awsResourceApiProvider;

  private final AwsSagemakerService awsSagemakerService;

  private final WsmMapper wsmMapper;

  public AwsAppsServiceImpl(
      WsmRetryHandler wsmRetryHandler,
      Provider<ResourceApi> resourceApiProvider,
      Provider<ControlledAwsResourceApi> awsResourceApiProvider,
      AwsSagemakerService awsSagemakerService,
      WsmMapper wsmMapper) {
    this.wsmRetryHandler = wsmRetryHandler;
    this.resourceApiProvider = resourceApiProvider;
    this.awsResourceApiProvider = awsResourceApiProvider;
    this.awsSagemakerService = awsSagemakerService;
    this.wsmMapper = wsmMapper;
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace) {

    CreateControlledAwsSageMakerNotebookResult sageMakerNotebookResult =
        wsmRetryHandler.run(
            context -> {
              CreateControlledAwsSageMakerNotebookRequestBody body =
                  new CreateControlledAwsSageMakerNotebookRequestBody()
                      .awsSageMakerNotebook(
                          new AwsSageMakerNotebookCreationParameters()
                              .instanceName("notebook-" + dbWorkspace.getName())
                              .region("us-east-1")
                              .instanceType("ml.t2.medium"))
                      .common(
                          createCommonFields(
                              dbWorkspace.getName() + "-sagemakernb",
                              dbWorkspace.getWorkspaceNamespace(),
                              AccessScope.PRIVATE_ACCESS,
                              CloningInstructionsEnum.NOTHING));
              body.setJobControl(new JobControl().id(UUID.randomUUID().toString()));
              return awsResourceApiProvider
                  .get()
                  .createAwsSageMakerNotebook(
                      body, UUID.fromString(dbWorkspace.getFirecloudUuid()));
            });

    logger.info(
        "Creating sagemaker notebook with job ID: {} , url is: {}",
        sageMakerNotebookResult.getJobReport().getId(),
        sageMakerNotebookResult.getJobReport().getResultURL());
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk) {
    Optional<ResourceDescription> notebookMaybe = getNotebookMaybe(appName, dbWorkspace);

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
                    UUID.fromString(dbWorkspace.getFirecloudUuid()),
                    resourceId));
  }

  @Override
  public UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace) {
    Optional<ResourceDescription> notebookMaybe = getNotebookMaybe(appName, dbWorkspace);

    if (notebookMaybe.isPresent()) {
      ResourceDescription notebook = notebookMaybe.get();
      UserAppEnvironment userAppEnvironment = wsmMapper.toApiApp(notebook);
      userAppEnvironment.appType(AppType.SAGEMAKER);

      setProxyURLs(dbWorkspace, notebook, userAppEnvironment);

      return userAppEnvironment;
    }

    return null;
  }

  private Optional<ResourceDescription> getNotebookMaybe(String appName, DbWorkspace dbWorkspace) {
    return wsmRetryHandler.run(
        context -> {
          ResourceList resourceList =
              resourceApiProvider
                  .get()
                  .enumerateResources(
                      UUID.fromString(dbWorkspace.getFirecloudUuid()),
                      0,
                      10,
                      ResourceType.AWS_SAGEMAKER_NOTEBOOK,
                      StewardshipType.CONTROLLED);
          return resourceList.getResources().stream()
              .filter(resource -> resource.getMetadata().getName().equals(appName))
              .findFirst();
        });
  }

  private void setProxyURLs(
      DbWorkspace dbWorkspace,
      ResourceDescription notebook,
      UserAppEnvironment userAppEnvironment) {
    try {
      AwsCredential awsCredential =
          awsResourceApiProvider
              .get()
              .getAwsSageMakerNotebookCredential(
                  UUID.fromString(dbWorkspace.getFirecloudUuid()),
                  notebook.getMetadata().getResourceId(),
                  AwsCredentialAccessScope.READ_ONLY,
                  900);
      NotebookInstanceStatus sageMakerNotebookInstanceStatus =
          awsSagemakerService.getSageMakerNotebookInstanceStatus(
              notebook.getResourceAttributes().getAwsSageMakerNotebook().getInstanceName(),
              awsCredential);
      if (!sageMakerNotebookInstanceStatus.equals(NotebookInstanceStatus.IN_SERVICE)) {
        return;
      }
      URL awsSageMakerProxyURL =
          awsSagemakerService.getAwsSageMakerProxyURL(
              notebook.getResourceAttributes().getAwsSageMakerNotebook().getInstanceName(),
              awsCredential);
      userAppEnvironment.proxyUrls(Map.of("app", awsSageMakerProxyURL.toString()));
    } catch (ApiException e) {
      throw new WorkbenchException(e);
    }
  }

  @Override
  public void updateApp(String appName, UserAppEnvironment app, DbWorkspace dbWorkspace) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public List<UserAppEnvironment> listAppsInWorkspace(DbWorkspace dbWorkspace) {
    ResourceList resourceList =
        wsmRetryHandler.run(
            context -> {
              return resourceApiProvider
                  .get()
                  .enumerateResources(
                      UUID.fromString(dbWorkspace.getFirecloudUuid()),
                      0,
                      20,
                      ResourceType.AWS_SAGEMAKER_NOTEBOOK,
                      StewardshipType.CONTROLLED);
            });
    List<ResourceDescription> resources = resourceList.getResources();
    if (resources.isEmpty()) {
      return Collections.emptyList();
    }
    UserAppEnvironment userAppEnvironment = wsmMapper.toApiApps(resourceList);
    resources.forEach(resource -> setProxyURLs(dbWorkspace, resource, userAppEnvironment));
    return List.of(userAppEnvironment);
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
