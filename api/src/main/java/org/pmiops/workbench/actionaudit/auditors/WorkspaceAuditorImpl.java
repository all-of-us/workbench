package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.inject.Provider;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.ModelBackedTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.WorkspaceTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuditorImpl implements WorkspaceAuditor {

  private final Provider<DbUser> userProvider;
  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private final Provider<String> actionIdProvider;

  @Autowired
  public WorkspaceAuditorImpl(
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
  public void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId) {
    CommonAuditEventInfo info = getAuditEventInfo();
    if (info == null) return;
    Map<String, String> propertyValues =
        ModelBackedTargetProperty.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), createdWorkspace);
    List<ActionAuditEvent> events =
        propertyValues.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .actionId(info.actionId())
                        .agentEmailMaybe(info.userEmail())
                        .actionType(ActionType.CREATE)
                        .agentType(AgentType.USER)
                        .agentIdMaybe(info.userId())
                        .targetType(TargetType.WORKSPACE)
                        .targetPropertyMaybe(entry.getKey())
                        .targetIdMaybe(dbWorkspaceId)
                        .newValueMaybe(entry.getValue())
                        .timestamp(info.timestamp())
                        .build())
            .collect(Collectors.toList());
    actionAuditService.send(events);
  }

  @Override
  public void fireEditAction(
      Workspace previousWorkspace, Workspace editedWorkspace, long workspaceId) {
    if (previousWorkspace == null || editedWorkspace == null) {
      logger.log(Level.WARNING, "Null workspace passed to fireEditAction.");
      return;
    }
    CommonAuditEventInfo info = getAuditEventInfo();
    if (info == null) return;
    var propertyToChangedValue =
        ModelBackedTargetProperty.getChangedValuesByName(
            WorkspaceTargetProperty.values(), previousWorkspace, editedWorkspace);
    List<ActionAuditEvent> actionAuditEvents =
        propertyToChangedValue.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .actionId(info.actionId())
                        .agentEmailMaybe(info.userEmail())
                        .actionType(ActionType.EDIT)
                        .agentType(AgentType.USER)
                        .agentIdMaybe(info.userId())
                        .targetType(TargetType.WORKSPACE)
                        .targetPropertyMaybe(entry.getKey())
                        .targetIdMaybe(workspaceId)
                        .previousValueMaybe(entry.getValue().previousValue())
                        .newValueMaybe(entry.getValue().newValue())
                        .timestamp(info.timestamp())
                        .build())
            .collect(Collectors.toList());
    actionAuditService.send(actionAuditEvents);
  }

  // Because the deleteWorkspace() method operates at the DB level, this method
  // has a different signature than for the create action. This makes keeping the properties
  // consistent across actions somewhat challenging.
  @Override
  public void fireDeleteAction(DbWorkspace dbWorkspace) {
    CommonAuditEventInfo info = getAuditEventInfo();
    if (info == null) return;
    ActionAuditEvent event =
        ActionAuditEvent.builder()
            .actionId(info.actionId())
            .agentEmailMaybe(info.userEmail())
            .actionType(ActionType.DELETE)
            .agentType(AgentType.USER)
            .agentIdMaybe(info.userId())
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(dbWorkspace.getWorkspaceId())
            .timestamp(info.timestamp())
            .build();
    actionAuditService.send(event);
  }

  @Override
  public void fireDuplicateAction(
      long sourceWorkspaceId, long destinationWorkspaceId, Workspace destinationWorkspace) {
    // We represent the duplication as a single action with events with different
    // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
    // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
    // out values that are the same.
    // TODO(jaycarlton): consider grabbing source event properties as well
    CommonAuditEventInfo info = getAuditEventInfo();
    if (info == null) return;
    ActionAuditEvent sourceEvent =
        ActionAuditEvent.builder()
            .actionId(info.actionId())
            .agentEmailMaybe(info.userEmail())
            .actionType(ActionType.DUPLICATE_FROM)
            .agentType(AgentType.USER)
            .agentIdMaybe(info.userId())
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(sourceWorkspaceId)
            .timestamp(info.timestamp())
            .build();
    Map<String, String> destinationPropertyValues =
        ModelBackedTargetProperty.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), destinationWorkspace);
    List<ActionAuditEvent> destinationEvents =
        destinationPropertyValues.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .actionId(info.actionId())
                        .agentEmailMaybe(info.userEmail())
                        .actionType(ActionType.DUPLICATE_TO)
                        .agentType(AgentType.USER)
                        .agentIdMaybe(info.userId())
                        .targetType(TargetType.WORKSPACE)
                        .targetPropertyMaybe(entry.getKey())
                        .targetIdMaybe(destinationWorkspaceId)
                        .newValueMaybe(entry.getValue())
                        .timestamp(info.timestamp())
                        .build())
            .toList();
    List<ActionAuditEvent> allEvents = new ArrayList<>(List.of(sourceEvent));
    allEvents.addAll(destinationEvents);
    actionAuditService.send(allEvents);
  }

  // We fire at least two events for this action, one with a target type of workspace,
  // and one with a target type of user for each invited or uninvited user. We'll include the
  // sharing user just so we have the whole access list at each sharing.
  @Override
  public void fireCollaborateAction(long sourceWorkspaceId, Map<Long, String> aclStringsByUserId) {
    if (aclStringsByUserId.isEmpty()) {
      logger.warning("Attempted to send collaboration event with empty ACL");
      return;
    }
    CommonAuditEventInfo info = getAuditEventInfo();
    if (info == null) return;
    ActionAuditEvent workspaceTargetEvent =
        ActionAuditEvent.builder()
            .actionId(info.actionId())
            .actionType(ActionType.COLLABORATE)
            .agentType(AgentType.USER)
            .agentEmailMaybe(info.userEmail())
            .agentIdMaybe(info.userId())
            .timestamp(info.timestamp())
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(sourceWorkspaceId)
            .build();
    List<ActionAuditEvent> inviteeEvents =
        aclStringsByUserId.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .actionId(info.actionId())
                        .actionType(ActionType.COLLABORATE)
                        .agentType(AgentType.USER)
                        .agentEmailMaybe(info.userEmail())
                        .agentIdMaybe(info.userId())
                        .timestamp(info.timestamp())
                        .targetType(TargetType.USER)
                        .targetIdMaybe(entry.getKey())
                        .targetPropertyMaybe(AclTargetProperty.ACCESS_LEVEL.name())
                        .newValueMaybe(entry.getValue())
                        .build())
            .toList();
    List<ActionAuditEvent> allEvents = new ArrayList<>(List.of(workspaceTargetEvent));
    allEvents.addAll(inviteeEvents);
    actionAuditService.send(allEvents);
  }

  @Nullable
  private CommonAuditEventInfo getAuditEventInfo() {
    DbUser dbUser = userProvider.get();
    if (dbUser == null) {
      logger.info("No user available for this action.");
      return null;
    }
    return new CommonAuditEventInfo(
        actionIdProvider.get(), dbUser.getUserId(), dbUser.getUsername(), clock.millis());
  }

  private static final Logger logger = Logger.getLogger(WorkspaceAuditorImpl.class.getName());
}
