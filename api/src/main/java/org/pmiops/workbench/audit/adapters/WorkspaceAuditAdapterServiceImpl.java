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
import org.pmiops.workbench.audit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.audit.targetproperties.WorkspaceTargetProperty;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils;
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
      Provider<User> userProvider, ActionAuditService actionAuditService, Clock clock) {
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
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final long timestamp = clock.millis();
      final ActionAuditEventImpl event =
          new ActionAuditEventImpl.Builder()
              .setActionId(actionId)
              .setAgentEmail(userEmail)
              .setActionType(ActionType.DELETE)
              .setAgentType(AgentType.USER)
              .setAgentId(userId)
              .setTargetType(TargetType.WORKSPACE)
              .setTargetId(dbWorkspace.getWorkspaceId())
              .setTimestamp(timestamp)
              .build();
      actionAuditService.send(event);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  @Override
  public void fireDuplicateAction(
      org.pmiops.workbench.db.model.Workspace sourceWorkspaceDbModel,
      org.pmiops.workbench.db.model.Workspace destinationWorkspaceDbModel) {
    try {
      // We represent the duplication as a single action with events with different
      // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
      // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
      // out values that are the same.
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final long timestamp = clock.millis();
      final ActionAuditEvent sourceEvent =
          ActionAuditEventImpl.builder()
              .setActionId(actionId)
              .setAgentEmail(userEmail)
              .setActionType(ActionType.DUPLICATE_FROM)
              .setAgentType(AgentType.USER)
              .setAgentId(userId)
              .setTargetType(TargetType.WORKSPACE)
              .setTargetId(sourceWorkspaceDbModel.getWorkspaceId())
              .setTimestamp(timestamp)
              .build();
      final Map<String, String> propertyValues =
          WorkspaceTargetProperty.getPropertyValuesByName(
              WorkspaceConversionUtils.toApiWorkspace(destinationWorkspaceDbModel));

      ImmutableList<ActionAuditEvent> destinationEvents =
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      ActionAuditEventImpl.builder()
                          .setActionId(actionId)
                          .setAgentEmail(userEmail)
                          .setActionType(ActionType.DUPLICATE_TO)
                          .setAgentType(AgentType.USER)
                          .setAgentId(userId)
                          .setTargetType(TargetType.WORKSPACE)
                          .setTargetProperty(entry.getKey())
                          .setTargetId(destinationWorkspaceDbModel.getWorkspaceId())
                          .setNewValue(entry.getValue())
                          .setTimestamp(timestamp)
                          .build())
              .collect(ImmutableList.toImmutableList());

      ImmutableList.Builder<ActionAuditEvent> allEventsBuilder = new ImmutableList.Builder<>();
      allEventsBuilder.add(sourceEvent);
      allEventsBuilder.addAll(destinationEvents);
      actionAuditService.send(allEventsBuilder.build());
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  // We fire at least two events for this action, one with a target type of workspace,
  // and one with a target type of user for each invited or uninvited user. We'll include the
  // sharing user just so we have the whole access list at each sharing.
  @Override
  public void fireCollaborateAction(long sourceWorkspaceId, Map<Long, String> aclStringsByUserId) {
    try {
      if (aclStringsByUserId.isEmpty()) {
        logger.warning("Attempted to send collaboration event with empty ACL");
        return;
      }

      final String actionId = ActionAuditService.newActionId();
      final long sharingUserId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final long timestamp = clock.millis();

      final ActionAuditEvent workspaceTargetEvent =
          ActionAuditEventImpl.builder()
              .setActionId(actionId)
              .setActionType(ActionType.COLLABORATE)
              .setAgentType(AgentType.USER)
              .setAgentEmail(userEmail)
              .setAgentId(sharingUserId)
              .setTimestamp(timestamp)
              .setTargetType(TargetType.WORKSPACE)
              .setTargetId(sourceWorkspaceId)
              .build();

      ImmutableList<ActionAuditEvent> inviteeEvents =
          aclStringsByUserId.entrySet().stream()
              .map(
                  entry ->
                      ActionAuditEventImpl.builder()
                          .setActionId(actionId)
                          .setActionType(ActionType.COLLABORATE)
                          .setAgentType(AgentType.USER)
                          .setAgentEmail(userEmail)
                          .setAgentId(sharingUserId)
                          .setTimestamp(timestamp)
                          .setTargetType(TargetType.USER)
                          .setTargetId(entry.getKey())
                          .setTargetProperty(AclTargetProperty.ACCESS_LEVEL.name())
                          .setNewValue(entry.getValue())
                          .build())
              .collect(ImmutableList.toImmutableList());

      ImmutableList.Builder<ActionAuditEvent> allEventsBuilder = new ImmutableList.Builder<>();
      allEventsBuilder.add(workspaceTargetEvent);
      allEventsBuilder.addAll(inviteeEvents);

      actionAuditService.send(allEventsBuilder.build());
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }
}
