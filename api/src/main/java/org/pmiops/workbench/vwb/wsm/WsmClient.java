package org.pmiops.workbench.vwb.wsm;

import jakarta.inject.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.wsmanager.ApiException;
import org.pmiops.workbench.wsmanager.api.WorkspaceApi;
import org.pmiops.workbench.wsmanager.model.CloneWorkspaceV2Request;
import org.pmiops.workbench.wsmanager.model.CloneWorkspaceV2Result;
import org.pmiops.workbench.wsmanager.model.CreateWorkspaceV2Request;
import org.pmiops.workbench.wsmanager.model.CreateWorkspaceV2Result;
import org.pmiops.workbench.wsmanager.model.GrantRoleRequestBody;
import org.pmiops.workbench.wsmanager.model.IamRole;
import org.pmiops.workbench.wsmanager.model.JobControl;
import org.pmiops.workbench.wsmanager.model.JobReport;
import org.pmiops.workbench.wsmanager.model.Properties;
import org.pmiops.workbench.wsmanager.model.Property;
import org.pmiops.workbench.wsmanager.model.State;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WsmClient {

  private static final Logger logger = LoggerFactory.getLogger(WsmClient.class);

  private final Provider<WorkspaceApi> workspaceServiceApi;

  private final WsmRetryHandler wsmRetryHandler;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public WsmClient(
      @Qualifier(WsmConfig.WSM_SERVICE_ACCOUNT_WORKSPACE_API)
          Provider<WorkspaceApi> workspaceServiceApi,
      WsmRetryHandler wsmRetryHandler,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workspaceServiceApi = workspaceServiceApi;
    this.wsmRetryHandler = wsmRetryHandler;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Clones a workspace as a service account.
   *
   * @param sourceWorkspaceUUID UUID of the source workspace
   * @param targetWorkspace Target workspace object
   * @param podId
   * @return WorkspaceDescription of the cloned workspace
   */
  public WorkspaceDescription cloneWorkspaceAsService(
      String sourceWorkspaceUUID, Workspace targetWorkspace, String podId) {

    return wsmRetryHandler.run(
        context -> {
          CloneWorkspaceV2Request cloneWorkspaceV2Request =
              new CloneWorkspaceV2Request()
                  .userFacingId(targetWorkspace.getNamespace())
                  .displayName(targetWorkspace.getName())
                  .additionalProperties(
                      stringMapToProperties(
                          Map.of(
                              "terra-workspace-short-description",
                              targetWorkspace.getResearchPurpose().getOtherPurposeDetails())))
                  .description(formatWorkspaceDescription(targetWorkspace))
                  .cloudResourceGroupId(podId)
                  .organizationId(workbenchConfigProvider.get().vwb.organizationId)
                  .jobControl(new JobControl().id(UUID.randomUUID().toString()));

          CloneWorkspaceV2Result cloneWorkspaceV2Result =
              workspaceServiceApi
                  .get()
                  .cloneWorkspaceV2(cloneWorkspaceV2Request, sourceWorkspaceUUID);
          JobReport jobReport = cloneWorkspaceV2Result.getJobReport();
          if (jobReport != null) {
            logger.error(" Status code from WSM {}", jobReport.getStatusCode());
          }

          Optional.ofNullable(cloneWorkspaceV2Result.getErrorReport())
              .ifPresent(
                  errorReport -> {
                    logger.error(
                        "Failed to clone workspace {}: {}",
                        sourceWorkspaceUUID,
                        errorReport.getMessage());
                    throw new WorkbenchException(
                        String.format(
                            "Failed to clone workspace %s: %s",
                            sourceWorkspaceUUID, errorReport.getMessage()));
                  });

          // call the get workspace endpoint to get the full description object
          return workspaceServiceApi
              .get()
              .getWorkspace(
                  cloneWorkspaceV2Result.getCloneResult().getDestinationWorkspaceId().toString(),
                  null);
        });
  }

  /**
   * Creates a new VWB workspace as a service account.
   *
   * @param targetWorkspace the RW workspace used to populate metadata
   * @param podId the cloud resource group (pod) id
   * @return WorkspaceDescription of the created workspace
   */
  public WorkspaceDescription createWorkspaceAsService(Workspace targetWorkspace, String podId) {

    return wsmRetryHandler.run(
        context -> {
          CreateWorkspaceV2Request createWorkspaceRequest =
              new CreateWorkspaceV2Request()
                  .userFacingId(targetWorkspace.getNamespace())
                  .displayName(targetWorkspace.getName())
                  .properties(
                      stringMapToProperties(
                          Map.of(
                              "terra-workspace-short-description",
                              targetWorkspace.getResearchPurpose().getOtherPurposeDetails())))
                  .description(formatWorkspaceDescription(targetWorkspace))
                  .cloudResourceGroupId(podId)
                  .organizationId(workbenchConfigProvider.get().vwb.organizationId)
                  .jobControl(new JobControl().id(UUID.randomUUID().toString()));

          CreateWorkspaceV2Result result =
              workspaceServiceApi.get().createWorkspaceV2(createWorkspaceRequest);

          JobReport jobReport = result.getJobReport();
          if (jobReport != null) {
            logger.error("Status code from WSM {}", jobReport.getStatusCode());
          }

          Optional.ofNullable(result.getErrorReport())
              .ifPresent(
                  errorReport -> {
                    logger.error(
                        "Failed to create workspace {}: {}",
                        targetWorkspace.getNamespace(),
                        errorReport.getMessage());
                    throw new WorkbenchException(
                        String.format(
                            "Failed to create workspace %s: %s",
                            targetWorkspace.getNamespace(), errorReport.getMessage()));
                  });

          // Fetch full workspace description
          return workspaceServiceApi.get().getWorkspace(result.getWorkspaceId().toString(), null);
        });
  }

  public void shareWorkspaceAsService(String workspaceId, String userEmail, IamRole role) {
    wsmRetryHandler.run(
        context -> {
          GrantRoleRequestBody grantRoleRequestBody =
              new GrantRoleRequestBody().memberEmail(userEmail);
          workspaceServiceApi.get().grantRole(grantRoleRequestBody, workspaceId, role);
          return null;
        });
  }

  /**
   * Waits for the workspace creation to complete.
   *
   * @param workspaceId ID of the workspace
   * @throws InterruptedException if the thread is interrupted
   * @throws ApiException if there is an API error
   */
  public void waitForWorkspaceCreation(String workspaceId)
      throws InterruptedException, ApiException {
    Duration timeout = Duration.ofMinutes(2);
    Duration pollInterval = Duration.ofSeconds(10);
    Instant deadline = Instant.now().plus(timeout);
    State state = State.CREATING;

    for (Instant now = Instant.now(); now.isBefore(deadline); now = Instant.now()) {
      WorkspaceDescription workspaceDescription =
          workspaceServiceApi.get().getWorkspace(workspaceId, null);
      state = workspaceDescription.getOperationState().getState();
      logger.debug("Vwb Workspace state: {}", state);

      if (state == State.READY) {
        return;
      }

      if (state == State.BROKEN) {
        throw new WorkbenchException("Vwb Workspace creation failed with state: " + state);
      }

      Thread.sleep(pollInterval.toMillis());
    }

    throw new WorkbenchException("Workspace creation is still in progress after timeout");
  }

  /**
   * Retrieves the workspace description as a service.
   *
   * @param workspaceNamespace Namespace of the workspace
   * @return WorkspaceDescription of the workspace
   */
  public WorkspaceDescription getWorkspaceAsService(String workspaceNamespace) {
    return wsmRetryHandler.run(
        context ->
            workspaceServiceApi
                .get()
                .getWorkspaceByUserFacingId(workspaceNamespace, IamRole.READER));
  }

  /**
   * Deletes a workspace resource using service account credentials.
   *
   * @param workspaceId UUID of the workspace
   * @param resourceId UUID of the resource to delete
   * @param resourceType Type of the resource (GCE_INSTANCE, DATAPROC_CLUSTER, etc.)
   */
  public void deleteWorkspaceResource(String workspaceId, String resourceId, String resourceType) {
    logger.info(
        String.format(
            "Deleting workspace resource: workspaceId=%s, resourceId=%s, resourceType=%s",
            workspaceId, resourceId, resourceType));
    try {
      wsmRetryHandler.run(
          context -> {
            org.pmiops.workbench.wsmanager.api.ControlledGcpResourceApi controlledGcpResourceApi =
                new org.pmiops.workbench.wsmanager.api.ControlledGcpResourceApi(
                    workspaceServiceApi.get().getApiClient());

            UUID resourceUuid = UUID.fromString(resourceId);
            JobControl jobControl = new JobControl().id(UUID.randomUUID().toString());

            try {
              switch (resourceType) {
                case "GCE_INSTANCE":
                  org.pmiops.workbench.wsmanager.model.DeleteControlledGcpGceInstanceRequest
                      gceRequest =
                          new org.pmiops.workbench.wsmanager.model
                                  .DeleteControlledGcpGceInstanceRequest()
                              .jobControl(jobControl);
                  controlledGcpResourceApi.deleteGceInstance(gceRequest, workspaceId, resourceUuid);
                  break;
                case "DATAPROC_CLUSTER":
                  org.pmiops.workbench.wsmanager.model.DeleteControlledGcpDataprocClusterRequest
                      dataprocRequest =
                          new org.pmiops.workbench.wsmanager.model
                                  .DeleteControlledGcpDataprocClusterRequest()
                              .jobControl(jobControl);
                  controlledGcpResourceApi.deleteDataprocCluster(
                      dataprocRequest, workspaceId, resourceUuid);
                  break;
                case "GCS_BUCKET":
                  org.pmiops.workbench.wsmanager.model.DeleteControlledGcpGcsBucketRequest
                      bucketRequest =
                          new org.pmiops.workbench.wsmanager.model
                                  .DeleteControlledGcpGcsBucketRequest()
                              .jobControl(jobControl);
                  controlledGcpResourceApi.deleteBucket(bucketRequest, workspaceId, resourceUuid);
                  break;
                case "BIG_QUERY_DATASET":
                  controlledGcpResourceApi.deleteBigQueryDataset(workspaceId, resourceUuid);
                  break;
                default:
                  throw new WorkbenchException(
                      String.format("Unsupported resource type: %s", resourceType));
              }

              return null;
            } catch (ApiException e) {
              // Rethrow ApiException so retry handler can handle it
              throw e;
            }
          });
    } catch (WorkbenchException e) {
      // Re-throw WorkbenchException from retry handler
      throw e;
    }
  }

  /**
   * Enumerates workspace resources using service account credentials. This method calls the
   * workspace manager API directly since the operation isn't available in the generated client.
   *
   * @param workspaceId UUID of the workspace
   * @return Object containing the workspace resources (as returned by workspace manager API)
   */
  public Object enumerateWorkspaceResources(String workspaceId) {
    logger.info(String.format("Enumerating workspace resources for workspaceId=%s", workspaceId));
    try {
      return wsmRetryHandler.run(
          context -> {
            try {
              // Call workspace manager API directly to enumerate resources
              // The workspace service API provider has service account credentials
              org.pmiops.workbench.wsmanager.api.ResourceApi resourceApi =
                  new org.pmiops.workbench.wsmanager.api.ResourceApi(
                      workspaceServiceApi.get().getApiClient());
              return resourceApi.enumerateResources(
                  workspaceId,
                  0, // offset
                  1000, // limit
                  null, // resource type (null = all types)
                  org.pmiops.workbench.wsmanager.model.StewardshipType.CONTROLLED,
                  false // includeAccessInfo
                  );
            } catch (ApiException e) {
              // Rethrow ApiException so retry handler can handle it
              throw e;
            }
          });
    } catch (WorkbenchException e) {
      // Re-throw WorkbenchException from retry handler
      throw e;
    }
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

  private String formatWorkspaceDescription(Workspace targetWorkspace) {
    String intendedStudy = targetWorkspace.getResearchPurpose().getIntendedStudy();
    String scientificApproach = targetWorkspace.getResearchPurpose().getScientificApproach();
    String anticipatedFindings = targetWorkspace.getResearchPurpose().getAnticipatedFindings();
    StringBuilder description = new StringBuilder();
    description
        .append("# Scientific Questions being studied\n")
        .append(intendedStudy)
        .append("\n\n");
    description.append("# Scientific Approaches\n").append(scientificApproach).append("\n\n");
    description.append("# Anticipated Findings\n").append(anticipatedFindings).append("\n\n");
    return description.toString();
  }
}
