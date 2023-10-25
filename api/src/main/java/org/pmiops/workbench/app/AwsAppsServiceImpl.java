package org.pmiops.workbench.app;

import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.aws.sagemaker.AwsSagemakerService;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.utils.mappers.WsmMapper;
import org.pmiops.workbench.wsm.WsmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

@Service("awsAppsService")
public class AwsAppsServiceImpl implements AppsService {

  private static final Logger logger = LoggerFactory.getLogger(AwsAppsServiceImpl.class);

  private final WsmClient wsmClient;

  private final AwsSagemakerService awsSagemakerService;

  private final WsmMapper wsmMapper;

  public AwsAppsServiceImpl(
      WsmClient wsmClient, AwsSagemakerService awsSagemakerService, WsmMapper wsmMapper) {
    this.wsmClient = wsmClient;
    this.awsSagemakerService = awsSagemakerService;
    this.wsmMapper = wsmMapper;
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace) {
    wsmClient.createSageMakerNotebookAsync(
        dbWorkspace.getFirecloudUuid(), dbWorkspace.getName(), dbWorkspace.getWorkspaceNamespace());
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk) {
    Optional<ResourceDescription> notebookMaybe =
        wsmClient.getSagemakerNotebookMaybe(appName, dbWorkspace.getFirecloudUuid());
    if (!notebookMaybe.isPresent()) {
      return;
    }
    try {
      AwsCredential awsCredential =
          wsmClient.getAwsSageMakerNotebookCredential(
              dbWorkspace.getFirecloudUuid(), notebookMaybe.get().getMetadata().getResourceId());
      String instanceName =
          notebookMaybe.get().getResourceAttributes().getAwsSageMakerNotebook().getInstanceName();

      NotebookInstanceStatus status =
          awsSagemakerService.getSageMakerNotebookInstanceStatus(instanceName, awsCredential);

      if (!NotebookInstanceStatus.STOPPED.equals(status)) {
        awsSagemakerService.stopSageMakerNotebookInstance(
            notebookMaybe.get().getResourceAttributes().getAwsSageMakerNotebook().getInstanceName(),
            awsCredential);
        int attempts = 0;
        while (attempts < 5) {
          status =
              awsSagemakerService.getSageMakerNotebookInstanceStatus(instanceName, awsCredential);
          if (status.equals(NotebookInstanceStatus.STOPPED)) {
            break;
          }
          TimeUnit.SECONDS.sleep(5); // Wait for 5 seconds before checking the status again
          attempts++;
        }
        if (!status.equals(NotebookInstanceStatus.STOPPED)) {
          return;
        }
      }

    } catch (ApiException e) {
      throw new WorkbenchException("Unable to stop sagemaker notebook in AWS", e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    wsmClient.deleteSagemakerNotebook(appName, dbWorkspace.getFirecloudUuid());
  }

  @Override
  public UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace) {
    Optional<ResourceDescription> notebookMaybe =
        wsmClient.getSagemakerNotebookMaybe(appName, dbWorkspace.getFirecloudUuid());

    if (notebookMaybe.isPresent()) {
      ResourceDescription notebook = notebookMaybe.get();
      UserAppEnvironment userAppEnvironment = wsmMapper.toApiApp(notebook);
      userAppEnvironment.appType(AppType.SAGEMAKER);

      setProxyURLs(dbWorkspace, notebook, userAppEnvironment);

      return userAppEnvironment;
    }

    return null;
  }

  private void setProxyURLs(
      DbWorkspace dbWorkspace,
      ResourceDescription notebook,
      UserAppEnvironment userAppEnvironment) {
    try {
      AwsCredential awsCredential =
          wsmClient.getAwsSageMakerNotebookCredential(
              dbWorkspace.getFirecloudUuid(), notebook.getMetadata().getResourceId());
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
        wsmClient.getResourcesInWorkspaceWithRetries(
            dbWorkspace.getFirecloudUuid(), ResourceType.AWS_SAGEMAKER_NOTEBOOK);
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
