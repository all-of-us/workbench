package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.PreviousNewValuePair;
import org.pmiops.workbench.actionaudit.targetproperties.ProfileTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.TargetPropertyExtractor;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProfileAuditorImpl implements ProfileAuditor {

  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private final Provider<String> actionIdProvider;

  @Autowired
  public ProfileAuditorImpl(
      ActionAuditService actionAuditService,
      Clock clock,
      @Qualifier(ACTION_ID_BEAN) Provider<String> actionIdProvider) {
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireCreateAction(Profile createdProfile) {
    Map<String, String> propertiesByName =
        TargetPropertyExtractor.getPropertyValuesByName(
            ProfileTargetProperty.values(), createdProfile);

    // We can't rely on the UserProvider here since the user didn't exist when it
    // got loaded or whatever.
    List<ActionAuditEvent> createEvents =
        propertiesByName.entrySet().stream()
            .map(
                property ->
                    ActionAuditEvent.builder()
                        .timestamp(clock.millis())
                        .actionId(actionIdProvider.get())
                        .actionType(ActionType.CREATE)
                        .agentType(AgentType.USER)
                        .agentIdMaybe(createdProfile.getUserId())
                        .agentEmailMaybe(createdProfile.getContactEmail())
                        .targetType(TargetType.PROFILE)
                        .targetIdMaybe(createdProfile.getUserId())
                        .targetPropertyMaybe(property.getKey())
                        .newValueMaybe(property.getValue())
                        .build())
            .collect(Collectors.toList());

    actionAuditService.send(createEvents);
  }

  @Override
  public void fireUpdateAction(Profile previousProfile, Profile updatedProfile, Agent agent) {
    Map<String, PreviousNewValuePair> propertiesByName =
        TargetPropertyExtractor.getChangedValuesByName(
            ProfileTargetProperty.values(), previousProfile, updatedProfile);

    List<ActionAuditEvent> events =
        propertiesByName.entrySet().stream()
            .map(
                entry ->
                    ActionAuditEvent.builder()
                        .timestamp(clock.millis())
                        .actionId(actionIdProvider.get())
                        .actionType(ActionType.EDIT)
                        .agentType(agent.agentType())
                        .agentIdMaybe(agent.idMaybe())
                        .agentEmailMaybe(agent.emailMaybe())
                        .targetType(TargetType.PROFILE)
                        .targetIdMaybe(previousProfile.getUserId())
                        .targetPropertyMaybe(entry.getKey())
                        .previousValueMaybe(entry.getValue().previousValue())
                        .newValueMaybe(entry.getValue().newValue())
                        .build())
            .collect(Collectors.toList());

    actionAuditService.send(events);
  }

  // Each user is assumed to have only one profile, but we can't rely on
  // the userProvider if the user is deleted before the profile.
  @Override
  public void fireDeleteAction(long userId, String userEmail) {
    ActionAuditEvent deleteProfileEvent =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.DELETE)
            .agentType(AgentType.USER)
            .agentIdMaybe(userId)
            .agentEmailMaybe(userEmail)
            .targetType(TargetType.PROFILE)
            .targetIdMaybe(userId)
            .build();

    actionAuditService.send(deleteProfileEvent);
  }

  @Override
  public void fireLoginAction(DbUser dbUser) {
    ActionAuditEvent loginEvent =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.LOGIN)
            .agentType(AgentType.USER)
            .agentIdMaybe(dbUser.getUserId())
            .agentEmailMaybe(dbUser.getUsername())
            .targetType(TargetType.WORKBENCH)
            .build();

    actionAuditService.send(loginEvent);
  }
}
