package org.pmiops.workbench.audit.adapters;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.audit.ActionAuditEvent;
import org.pmiops.workbench.audit.ActionAuditEventImpl;
import org.pmiops.workbench.audit.ActionAuditService;
import org.pmiops.workbench.audit.ActionType;
import org.pmiops.workbench.audit.AgentType;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.audit.targetproperties.WorkspaceTargetProperty;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuditAdapterServiceImpl implements WorkspaceAuditAdapterService {

  private static final Logger logger =
      Logger.getLogger(WorkspaceAuditAdapterServiceImpl.class.getName());

  private Provider<User> userProvider;
  private ActionAuditService actionAuditService;
  private Clock clock;

  @Autowired
  public WorkspaceAuditAdapterServiceImpl(
      Provider<User> userProvider,
      ActionAuditService actionAuditService,
      Clock clock) {
    this.userProvider = userProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
  }

  @Override
  public void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId) {
    try {
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final Map<String, String> propertyValues =
          WorkspaceTargetProperty.getPropertyValuesByName(createdWorkspace);
      final long timestamp = clock.millis();
      // omit the previous value column
      ImmutableList<ActionAuditEvent> events =
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      new ActionAuditEventImpl.Builder()
                          .setActionId(actionId)
                          .setAgentEmail(userEmail)
                          .setActionType(ActionType.CREATE)
                          .setAgentType(AgentType.USER)
                          .setAgentId(userId)
                          .setTargetType(TargetType.WORKSPACE)
                          .setTargetProperty(entry.getKey())
                          .setTargetId(dbWorkspaceId)
                          .setNewValue(entry.getValue())
                          .setTimestamp(timestamp)
                          .build())
              .collect(ImmutableList.toImmutableList());
      actionAuditService.send(events);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, e, () -> "Exception encountered during audit.");
  }

  // Because the deleteWorkspace() method operates at the DB level, this method
  // has a different signature than for the create action. This makes keeping the properties
  // consistent across actions somewhat challenging.
  @Override
  public void fireDeleteAction(org.pmiops.workbench.db.model.Workspace dbWorkspace) {
    try {
      fireSimpleWorkspaceEvent(dbWorkspace.getWorkspaceId(), ActionType.DELETE);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  @Override
  public void fireDuplicateAction(long sourceWorkspaceId, long destinationWorkspaceId) {
    try {
      // We represent the duplication as a single action with two events with different
      // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO.
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final long timestamp = clock.millis();
      final ActionAuditEvent sourceEvent = ActionAuditEventImpl.builder()
          .setActionId(actionId)
          .setAgentEmail(userEmail)
          .setActionType(ActionType.DUPLICATE_FROM)
          .setAgentType(AgentType.USER)
          .setAgentId(userId)
          .setTargetType(TargetType.WORKSPACE)
          .setTargetId(sourceWorkspaceId)
          .setTimestamp(timestamp)
          .build();
      final ActionAuditEvent destinationEvent = ActionAuditEventImpl.builder()
          .setActionId(actionId)
          .setAgentEmail(userEmail)
          .setActionType(ActionType.DUPLICATE_TO)
          .setAgentType(AgentType.USER)
          .setAgentId(userId)
          .setTargetType(TargetType.WORKSPACE)
          .setTargetId(destinationWorkspaceId)
          .setTimestamp(timestamp)
          .build();
      actionAuditService.send(ImmutableList.of(sourceEvent, destinationEvent));
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void fireSimpleWorkspaceEvent(long workspaceId,
      ActionType actionType) {
    final String actionId = ActionAuditService.newActionId();
    final long userId = userProvider.get().getUserId();
    final String userEmail = userProvider.get().getEmail();
    final long timestamp = clock.millis();
    final ActionAuditEventImpl event =
        new ActionAuditEventImpl.Builder()
            .setActionId(actionId)
            .setAgentEmail(userEmail)
            .setActionType(actionType)
            .setAgentType(AgentType.USER)
            .setAgentId(userId)
            .setTargetType(TargetType.WORKSPACE)
            .setTargetId(workspaceId)
            .setTimestamp(timestamp)
            .build();
    actionAuditService.send(event);
  }

}
