package org.pmiops.workbench.opsgenie;

import com.google.common.collect.ImmutableList;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.Transient;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.utils.Matchers;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressEventServiceImpl implements EgressEventService {

  private static final Logger logger = Logger.getLogger(EgressEventServiceImpl.class.getName());
  private static final Pattern VM_PREFIX_PATTERN = Pattern.compile("all-of-us-(?<userid>\\d+)");
  private static final String USER_ID_GROUP_NAME = "userid";
  private final Clock clock;
  private final EgressEventAuditor egressEventAuditor;
  private final InstitutionService institutionService;
  private final Provider<AlertApi> alertApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;
  private final WorkspaceAdminService workspaceAdminService;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public EgressEventServiceImpl(
      Clock clock,
      EgressEventAuditor egressEventAuditor,
      InstitutionService institutionService,
      Provider<AlertApi> alertApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      WorkspaceAdminService workspaceAdminService,
      WorkspaceDao workspaceDao) {
    this.clock = clock;
    this.egressEventAuditor = egressEventAuditor;
    this.institutionService = institutionService;
    this.alertApiProvider = alertApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.workspaceAdminService = workspaceAdminService;
    this.workspaceDao = workspaceDao;
  }

  @Override
  public void handleEvent(EgressEvent event) {
    logger.warning(
        String.format(
            "Received an egress event from project %s (%.2fMiB, VM prefix %s)",
            event.getProjectName(), event.getEgressMib(), event.getVmPrefix()));
    String namespace =
        workspaceDao
            .getByGoogleProject(event.getProjectName())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format(
                            "Workspace not found by given Google Project Id: %s",
                            event.getProjectName())))
            .getWorkspaceNamespace();
    event.setWorkspaceNamespace(namespace);
    this.egressEventAuditor.fireEgressEvent(event);
    this.createEgressEventAlert(event);
  }

  // Create (or potentially update) an OpsGenie alert for an egress event.
  private SuccessResponse createAlert(CreateAlertRequest createAlertRequest) throws ApiException {
    return this.alertApiProvider.get().createAlert(createAlertRequest);
  }

  private void createEgressEventAlert(EgressEvent egressEvent) {
    final CreateAlertRequest createAlertRequest = egressEventToOpsGenieAlert(egressEvent);
    try {
      final SuccessResponse response = createAlert(createAlertRequest);
      logger.info(
          String.format(
              "Successfully created or updated Opsgenie alert for high-egress event on project %s (Opsgenie request ID %s)",
              egressEvent.getProjectName(), response.getRequestId()));
    } catch (ApiException e) {
      logger.severe(
          String.format(
              "Error creating Opsgenie alert for egress event on project %s: %s",
              egressEvent.getProjectName(), e.getMessage()));
      e.printStackTrace();
    }
  }

  private CreateAlertRequest egressEventToOpsGenieAlert(EgressEvent egressEvent) {
    final CreateAlertRequest request = new CreateAlertRequest();
    request.setMessage(String.format("High-egress event (%s)", egressEvent.getProjectName()));
    request.setDescription(getDescription(egressEvent));

    // Add a note with some more specific details about the alerting criteria and threshold. Notes
    // are appended to an existing Opsgenie ticket if this request is de-duplicated against an
    // existing ticket, so they're a helpful way to summarize temporal updates to the status of
    // an incident.
    request.setNote(
        String.format(
            "Time window: %d secs, threshold: %.2f MiB, observed: %.2f MiB",
            egressEvent.getTimeWindowDuration(),
            egressEvent.getEgressMibThreshold(),
            egressEvent.getEgressMib()));
    request.setTags(ImmutableList.of("high-egress-event"));

    // Set the alias, which is Opsgenie's string key for alert de-duplication. See
    // https://docs.opsgenie.com/docs/alert-deduplication
    request.setAlias(egressEvent.getProjectName() + " | " + egressEvent.getVmPrefix());
    return request;
  }

  @NotNull
  private String getDescription(EgressEvent egressEvent) {
    final WorkspaceAdminView adminWorkspace =
        workspaceAdminService.getWorkspaceAdminView(egressEvent.getProjectName());
    final Workspace workspace = adminWorkspace.getWorkspace();
    final String creatorDetails =
        userService
            .getByUsername(workspace.getCreator())
            .map(this::getAdminDescription)
            .orElse("Creator not Found");

    final Optional<DbUser> executor =
        vmNameToUserDatabaseId(egressEvent.getVmPrefix()).flatMap(userService::getByDatabaseId);

    final String executorDetails =
        executor.map(this::getAdminDescription).orElse("Executing User not Found");

    final String collaboratorDetails =
        adminWorkspace.getCollaborators().stream()
            .map(this::formatWorkspaceUserAdminView)
            .collect(Collectors.joining("\n\n"));

    return String.format(
            "Workspace \"%s\", Age = %d Days\n",
            workspace.getName(), getAgeInDays(Instant.ofEpochMilli(workspace.getCreationTime())))
        + String.format(
            "Terra Billing Project/Firecloud Namespace: %s\n", egressEvent.getProjectName())
        + String.format("Google Project Id: %s\n", egressEvent.getProjectName())
        + String.format("Notebook server VM prefix: %s\n", egressEvent.getVmPrefix())
        + String.format("MySQL workspace_id: %d\n", adminWorkspace.getWorkspaceDatabaseId())
        + String.format(
            "Total egress detected: %.2f MiB in %d secs\n",
            egressEvent.getEgressMib(), egressEvent.getTimeWindowDuration())
        + String.format(
            "egress breakdown: GCE - %.2f MiB, Dataproc - %.2fMiB via master, %.2fMiB via workers\n\nn",
            egressEvent.getGceEgressMib(),
            egressEvent.getDataprocMasterEgressMib(),
            egressEvent.getDataprocWorkerEgressMib())
        + String.format(
            "Runtime Prefix: %s\n", executor.map(DbUser::getRuntimeName).orElse("unknown"))
        + String.format("User Running Notebook: %s\n\n", executorDetails)
        + String.format("Workspace Creator: %s\n\n", creatorDetails)
        + String.format("Collaborators: \n%s\n", collaboratorDetails)
        + String.format(
            "Workspace Admin Console (Prod Admin User): %s/admin/workspaces/%s/\n",
            workbenchConfigProvider.get().server.uiBaseUrl, egressEvent.getProjectName())
        + "Playbook Entry: https://broad.io/aou-high-egress-event";
  }

  private String formatWorkspaceUserAdminView(WorkspaceUserAdminView userAdminView) {
    final String userDetails =
        userService
            .getByDatabaseId(userAdminView.getUserDatabaseId())
            .map(this::getAdminDescription)
            .orElse(
                String.format(
                    "Collaborator with user_id %d not Found", userAdminView.getUserDatabaseId()));
    return String.format("%s: %s", userAdminView.getRole(), userDetails);
  }

  /**
   * Produce a succinct string for giving an administrator a heads-up in an alert or other text-only
   * context.
   */
  @Transient
  public String getAdminDescription(DbUser dbUser) {
    final String institution =
        institutionService.getByUser(dbUser).map(Institution::getDisplayName).orElse("not found");
    return String.format(
        "%s %s - Username: %s\n" + "user_id: %d, Institution: %s, Account Age: %d days",
        dbUser.getGivenName(),
        dbUser.getFamilyName(),
        dbUser.getUsername(),
        dbUser.getUserId(),
        institution,
        getAgeInDays(dbUser.getCreationTime().toInstant()));
  }

  private long getAgeInDays(Instant creationTime) {
    return Duration.between(creationTime, clock.instant()).toDays();
  }

  private Optional<Long> vmNameToUserDatabaseId(String vmPrefix) {
    return Matchers.getGroup(VM_PREFIX_PATTERN, vmPrefix, USER_ID_GROUP_NAME).map(Long::parseLong);
  }
}
