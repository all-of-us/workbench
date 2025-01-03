package org.pmiops.workbench.wsm;

import jakarta.inject.Provider;
import java.util.Map;
import java.util.UUID;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.wsmanager.ApiException;
import org.pmiops.workbench.wsmanager.api.WorkspaceApi;
import org.pmiops.workbench.wsmanager.model.*;
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
      @Qualifier(WsmConfig.SERVICE_ACCOUNT_WORKSPACE_API)
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
   * @return WorkspaceDescription of the cloned workspace
   */
  public WorkspaceDescription cloneWorkspaceAsService(
      String sourceWorkspaceUUID, Workspace targetWorkspace) {

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
                  .cloudResourceGroupId(workbenchConfigProvider.get().vwb.defaultPodId)
                  .organizationId(workbenchConfigProvider.get().vwb.organizationId)
                  .jobControl(new JobControl().id(UUID.randomUUID().toString()));

          CloneWorkspaceV2Result cloneWorkspaceV2Result =
              workspaceServiceApi
                  .get()
                  .cloneWorkspaceV2(cloneWorkspaceV2Request, sourceWorkspaceUUID);

          // call the get workspace endpoint to get the full description object
          return workspaceServiceApi
              .get()
              .getWorkspace(
                  cloneWorkspaceV2Result.getCloneResult().getDestinationWorkspaceId().toString(),
                  null);
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
    int retries = 0;
    State state = State.CREATING;
    // Wait for 2 minutes for the workspace creation before giving up
    do {
      WorkspaceDescription workspaceDescription =
          workspaceServiceApi.get().getWorkspace(workspaceId, null);
      state = workspaceDescription.getOperationState().getState();
      logger.debug("Workspace state: {}", state);
      if (state == State.READY) {
        return;
      }
      retries++;
      Thread.sleep(10_000);
    } while (State.BROKEN != state && retries < 12);
    throw new WorkbenchException("Workspace creation is still in progress");
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
