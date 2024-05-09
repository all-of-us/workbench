package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditEvent.Builder;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.DbEgressEventTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEscalationTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventCommentTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.ModelBackedTargetProperty;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.SumologicEgressEventRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EgressEventAuditorImpl implements EgressEventAuditor {
  private final Provider<DbUser> userProvider;
  private final ActionAuditService actionAuditService;
  private final WorkspaceService workspaceService;
  private final WorkspaceDao workspaceDao;
  private final UserDao userDao;
  private final Clock clock;
  private final Provider<String> actionIdProvider;

  private static final Logger logger = Logger.getLogger(EgressEventAuditorImpl.class.getName());

  @Autowired
  public EgressEventAuditorImpl(
      Provider<DbUser> userProvider,
      ActionAuditService actionAuditService,
      WorkspaceService workspaceService,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      Clock clock,
      @Qualifier(ACTION_ID_BEAN) Provider<String> actionIdProvider) {
    this.userProvider = userProvider;
    this.actionAuditService = actionAuditService;
    this.workspaceService = workspaceService;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
    this.clock = clock;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireEgressEvent(SumologicEgressEvent event) {
    DbWorkspace dbWorkspace = getDbWorkspace(event);

    String agentEmail = null;
    long agentId = 0;

    // Using service-level creds, load the FireCloud workspace ACLs to find all members
    // of the workspace. Then attempt to find the user who aligns with the named VM.
    // This logic is applicable in case of Runtimes only and not Apps, since Apps are designed
    // differently.
    // Apps use a shared GKE cluster and nodes, and therefore it's not possible to find the App
    // owner.
    var userRoles =
        workspaceService.getFirecloudUserRoles(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    // The user's runtime name is used as a common VM prefix across all Leo machine types, and
    // covers the following situations:
    // 1. GCE VMs: all-of-us-<user_id>
    // 2. Dataproc master nodes: all-of-us-<user_id>-m
    // 3. Dataproc worker nodes: all-of-us-<user_id>-w-<index>
    var vmOwner =
        userRoles.stream()
            .map(UserRole::getEmail)
            .map(userDao::findUserByUsername)
            .filter(user -> user.getRuntimeName().equals(event.getVmPrefix()))
            .findFirst()
            .orElse(null);

    if (vmOwner != null) {
      agentEmail = vmOwner.getUsername();
      agentId = vmOwner.getUserId();
    } else {
      // If the VM prefix doesn't match a user on the workspace, we'll still log an
      // event in the target workspace, but with nulled-out user info.
      logger.warning(
          String.format(
              "Could not find a user for VM name %s in namespace %s",
              event.getVmName(), dbWorkspace.getWorkspaceNamespace()));
    }

    fireEvent(agentId, agentEmail, dbWorkspace, event);
  }

  @Override
  public void fireEgressEventForUser(SumologicEgressEvent event, DbUser user) {
    DbWorkspace dbWorkspace = getDbWorkspace(event);

    String agentEmail = user.getUsername();
    long agentId = user.getUserId();

    fireEvent(agentId, agentEmail, dbWorkspace, event);
  }

  private void fireEvent(
      long agentId, String agentEmail, DbWorkspace dbWorkspace, SumologicEgressEvent event) {
    String actionId = actionIdProvider.get();
    Builder baseEventBuilder =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .actionId(actionId)
            .actionType(ActionType.DETECT_HIGH_EGRESS_EVENT)
            .agentType(AgentType.USER)
            .agentIdMaybe(agentId)
            .agentEmailMaybe(agentEmail)
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(dbWorkspace.getWorkspaceId());
    fireEventSet(baseEventBuilder, event, null);
  }

  private DbWorkspace getDbWorkspace(SumologicEgressEvent event) {
    // Load the workspace via the GCP project name
    var dbWorkspaceMaybe = workspaceDao.getByGoogleProject(event.getProjectName());
    if (dbWorkspaceMaybe.isEmpty()) {
      // Failing to find a workspace is odd enough that we want to return a non-success
      // response at the API level. But it's also worth storing a permanent record of the
      // high-egress event by logging it without a workspace target.
      fireFailedToFindWorkspace(event);
      throw new BadRequestException(
          String.format(
              "The workspace (Google Project Id '%s') referred to by the given event is no longer active or never existed.",
              event.getProjectName()));
    }
    return dbWorkspaceMaybe.get();
  }

  @Override
  public void fireRemediateEgressEvent(
      DbEgressEvent dbEvent, WorkbenchConfig.EgressAlertRemediationPolicy.Escalation escalation) {
    var dbWorkspaceMaybe = Optional.ofNullable(dbEvent.getWorkspace());
    var actionId = actionIdProvider.get();
    Builder baseEventBuilder =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .actionId(actionId)
            .actionType(ActionType.REMEDIATE_HIGH_EGRESS_EVENT)
            .agentType(AgentType.SYSTEM)
            .targetType(TargetType.WORKSPACE)
            .targetIdMaybe(dbWorkspaceMaybe.map(DbWorkspace::getWorkspaceId).orElse(null));
    fireRemediationEventSet(baseEventBuilder, dbEvent, escalation);
  }

  @Override
  public void fireAdminEditEgressEvent(DbEgressEvent previousEvent, DbEgressEvent updatedEvent) {
    var changesByProperty =
        ModelBackedTargetProperty.getChangedValuesByName(
            DbEgressEventTargetProperty.values(), previousEvent, updatedEvent);
    var dbWorkspaceMaybe = Optional.ofNullable(updatedEvent.getWorkspace());
    var actionId = actionIdProvider.get();
    var admin = userProvider.get();
    actionAuditService.send(
        changesByProperty.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .timestamp(clock.millis())
                        .actionId(actionId)
                        .actionType(ActionType.EDIT)
                        .agentType(AgentType.ADMINISTRATOR)
                        .agentIdMaybe(admin.getUserId())
                        .agentEmailMaybe(admin.getUsername())
                        .targetType(TargetType.WORKSPACE)
                        .targetIdMaybe(
                            dbWorkspaceMaybe.map(DbWorkspace::getWorkspaceId).orElse(null))
                        .targetPropertyMaybe(entry.getKey())
                        .previousValueMaybe(entry.getValue().previousValue())
                        .newValueMaybe(entry.getValue().newValue())
                        .build())
            .toList());
  }

  @Override
  public void fireFailedToParseEgressEventRequest(SumologicEgressEventRequest request) {
    fireEventSetWithoutEgressEvent(
        getGenericBaseEventBuilder(),
        String.format(
            "Failed to parse egress event JSON from SumoLogic. Field contents: %s",
            request.getEventsJsonArray()));
  }

  @Override
  public void fireBadApiKey(String apiKey, SumologicEgressEventRequest request) {
    fireEventSetWithoutEgressEvent(
        getGenericBaseEventBuilder(),
        String.format(
            "Received bad API key from SumoLogic. Bad key: %s, full request: %s",
            apiKey, request.toString()));
  }

  private void fireFailedToFindWorkspace(SumologicEgressEvent event) {
    fireEventSet(
        getGenericBaseEventBuilder(),
        event,
        String.format("Failed to find workspace for high-egress event: %s", event.toString()));
  }

  private void fireEventSetWithoutEgressEvent(Builder baseEventBuilder, @Nullable String comment) {
    fireEventSet(baseEventBuilder, null, comment);
  }

  /**
   * Creates and fires a set of events derived from the given base event, incorporating audit rows
   * to record properties of the egress event and a human-readable comment. Either the egress event
   * or the comment may be null, in which case those row(s) won't be generated.
   */
  private void fireEventSet(
      Builder baseEventBuilder,
      @Nullable SumologicEgressEvent egressEvent,
      @Nullable String comment) {
    var events = new ArrayList<ActionAuditEvent>();
    if (egressEvent != null) {
      var propertyValues =
          ModelBackedTargetProperty.getPropertyValuesByName(
              EgressEventTargetProperty.values(), egressEvent);
      events.addAll(
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      baseEventBuilder
                          .targetPropertyMaybe(entry.getKey())
                          .newValueMaybe(entry.getValue())
                          .build())
              .toList());
    }
    if (comment != null) {
      events.add(
          baseEventBuilder
              .targetPropertyMaybe(EgressEventCommentTargetProperty.COMMENT.getPropertyName())
              .newValueMaybe(comment)
              .build());
    }
    actionAuditService.send(events);
  }

  private void fireRemediationEventSet(
      Builder baseEventBuilder,
      @Nullable DbEgressEvent egressEvent,
      @Nullable WorkbenchConfig.EgressAlertRemediationPolicy.Escalation escalation) {
    var events = new ArrayList<ActionAuditEvent>();
    if (egressEvent != null) {
      var propertyValues =
          ModelBackedTargetProperty.getPropertyValuesByName(
              DbEgressEventTargetProperty.values(), egressEvent);
      events.addAll(
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      baseEventBuilder
                          .targetPropertyMaybe(entry.getKey())
                          .newValueMaybe(entry.getValue())
                          .build())
              .toList());
    }
    if (escalation != null) {
      var propertyValues =
          ModelBackedTargetProperty.getPropertyValuesByName(
              EgressEscalationTargetProperty.values(), escalation);
      events.addAll(
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      baseEventBuilder
                          .targetPropertyMaybe(entry.getKey())
                          .newValueMaybe(entry.getValue())
                          .build())
              .toList());
    }
    actionAuditService.send(events);
  }

  /**
   * Returns an ActionAuditEvent suitable for logging system-level error messages, e.g. when an
   * inbound high-egress event refers to an inactive workspace or when the request JSON could not be
   * successfully parsed.
   */
  private Builder getGenericBaseEventBuilder() {
    return ActionAuditEvent.builder()
        .timestamp(clock.millis())
        .actionId(actionIdProvider.get())
        .actionType(ActionType.DETECT_HIGH_EGRESS_EVENT)
        .agentType(AgentType.SYSTEM)
        .targetType(TargetType.WORKSPACE);
  }
}
