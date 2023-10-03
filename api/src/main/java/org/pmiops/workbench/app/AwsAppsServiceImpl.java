package org.pmiops.workbench.app;

import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.model.*;
import java.util.List;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.wsm.WsmRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("awsAppsService")
public class AwsAppsServiceImpl implements AppsService {

  private static final Logger logger = LoggerFactory.getLogger(AwsAppsServiceImpl.class);

  private final WsmRetryHandler wsmRetryHandler;

  private final Provider<ControlledAwsResourceApi> awsResourceApiProvider;

  public AwsAppsServiceImpl(
      WsmRetryHandler wsmRetryHandler, Provider<ControlledAwsResourceApi> awsResourceApiProvider) {
    this.wsmRetryHandler = wsmRetryHandler;
    this.awsResourceApiProvider = awsResourceApiProvider;
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace) {

    CreateControlledAwsSageMakerNotebookResult sageMakerNotebook =
        wsmRetryHandler.run(
            context -> {
              CreateControlledAwsSageMakerNotebookRequestBody body =
                  new CreateControlledAwsSageMakerNotebookRequestBody()
                      .awsSageMakerNotebook(
                          new AwsSageMakerNotebookCreationParameters()
                              .instanceName("notebook-" + dbWorkspace.getName())
                              .region("us-east-1")
                              .instanceType("ml.t2.medium")
                          /*.defaultBucket(
                          new AwsSagemakerNotebookDefaultBucket()
                                  .bucketId(bucketId)
                                  .accessScope(AwsCredentialAccessScope.WRITE_READ))*/
                          )
                      .common(
                          createCommonFields(
                              "sagemaker-nb-" + dbWorkspace.getName(),
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
        "Creating sagemaker notebook with ID: {} , url is: {}",
        sageMakerNotebook.getJobReport().getId(),
        sageMakerNotebook.getJobReport().getResultURL());
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk) {}

  @Override
  public UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace) {
    return null;
  }

  @Override
  public void updateApp(String appName, UserAppEnvironment app, DbWorkspace dbWorkspace) {}

  @Override
  public List<UserAppEnvironment> listAppsInWorkspace(DbWorkspace dbWorkspace) {
    return List.of();
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
