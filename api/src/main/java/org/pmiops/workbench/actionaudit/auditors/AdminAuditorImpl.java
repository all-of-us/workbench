package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditEvent.Builder;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditorImpl implements AdminAuditor {

  private final Provider<DbUser> userProvider;
  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private final Provider<String> actionIdProvider;

  @Autowired
  public AdminAuditorImpl(
      Provider<DbUser> userProvider,
      ActionAuditService actionAuditService,
      Clock clock,
      @Qualifier(ACTION_ID_BEAN) Provider<String> actionIdProvider) {
    this.userProvider = userProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireViewNotebookAction(
      String workspaceNamespace,
      String workspaceName,
      String notebookFilename,
      AccessReason accessReason) {
    DbUser dbUser = userProvider.get();
    String actionId = actionIdProvider.get();
    long timestamp = clock.millis();

    Map<String, String> props =
        Map.of(
            "workspace_namespace", workspaceNamespace,
            "workspace_name", workspaceName,
            "notebook_name", notebookFilename,
            "access_reason", accessReason.getReason());

    var events =
        props.entrySet().stream()
            .map(
                entry ->
                    new Builder()
                        .actionId(actionId)
                        .actionType(ActionType.VIEW)
                        .agentType(AgentType.ADMINISTRATOR)
                        .agentEmailMaybe(dbUser.getUsername())
                        .agentIdMaybe(dbUser.getUserId())
                        .targetType(TargetType.NOTEBOOK)
                        .targetPropertyMaybe(entry.getKey())
                        .newValueMaybe(entry.getValue())
                        .timestamp(timestamp)
                        .build())
            .toList();

    actionAuditService.send(events);
  }

  @Override
  public void fireLockWorkspaceAction(long workspaceId, AdminLockingRequest adminLockingRequest) {
    DbUser dbUser = userProvider.get();
    String actionId = actionIdProvider.get();
    long timestamp = clock.millis();
    String requestTimestamp = new Date(adminLockingRequest.getRequestDateInMillis()).toString();

    Map<String, String> props =
        Map.of(
            "locked",
            "true",
            "reason",
            adminLockingRequest.getRequestReason(),
            "request_date",
            requestTimestamp);

    var events =
        props.entrySet().stream()
            .map(
                entry ->
                    new Builder()
                        .actionId(actionId)
                        .actionType(ActionType.EDIT)
                        .agentType(AgentType.ADMINISTRATOR)
                        .agentEmailMaybe(dbUser.getUsername())
                        .agentIdMaybe(dbUser.getUserId())
                        .targetType(TargetType.WORKSPACE)
                        .targetIdMaybe(workspaceId)
                        .targetPropertyMaybe(entry.getKey())
                        .newValueMaybe(entry.getValue())
                        .timestamp(timestamp)
                        .build())
            .toList();

    actionAuditService.send(events);
  }

  @Override
  public void fireUnlockWorkspaceAction(long workspaceId) {
    DbUser dbUser = userProvider.get();
    String actionId = actionIdProvider.get();
    long timestamp = clock.millis();

    ActionAuditEvent event =
        new Builder()
            .actionId(actionId)
            .actionType(ActionType.EDIT)
            .agentType(AgentType.ADMINISTRATOR)
            .agentEmailMaybe(dbUser.getUsername())
            .agentIdMaybe(dbUser.getUserId())
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(workspaceId)
            .targetPropertyMaybe("locked")
            .newValueMaybe("false")
            .timestamp(timestamp)
            .build();

    actionAuditService.send(event);
  }
}
