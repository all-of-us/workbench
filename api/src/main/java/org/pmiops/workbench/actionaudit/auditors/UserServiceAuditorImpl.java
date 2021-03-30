package org.pmiops.workbench.actionaudit.auditors;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserServiceAuditorImpl implements UserServiceAuditor {

  private static final Logger log = Logger.getLogger(UserServiceAuditorImpl.class.getName());

  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private Provider<DbUser> dbUserProvider;
  private Provider<String> actionIdProvider;

  @Autowired
  public UserServiceAuditorImpl(
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> dbUserProvider,
      @Qualifier("ACTION_ID") Provider<String> actionIdProvider) {
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.dbUserProvider = dbUserProvider;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireUpdateAccessTiersAction(
      DbUser targetUser,
      List<DbAccessTier> previousAccessTiers,
      List<DbAccessTier> newAccessTiers,
      Agent agent) {
    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(agent.getAgentType())
            .agentIdMaybe(agent.getIdMaybe())
            .agentEmailMaybe(agent.getEmailMaybe())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.EDIT)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(AccountTargetProperty.ACCESS_TIERS.getPropertyName())
            .targetIdMaybe(targetUser.getUserId())
            .previousValueMaybe(tiersToString(previousAccessTiers))
            .newValueMaybe(tiersToString(newAccessTiers))
            .build());
  }

  private String tiersToString(List<DbAccessTier> tiers) {
    return tiers.stream().sorted().map(DbAccessTier::getShortName).collect(Collectors.joining(","));
  }

  @Override
  public void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Optional<Instant> previousBypassTime,
      Optional<Instant> newBypassTime) {
    DbUser adminUser = dbUserProvider.get();
    ActionAuditEvent.Builder eventBuilder =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.ADMINISTRATOR)
            .agentIdMaybe(adminUser.getUserId())
            .agentEmailMaybe(adminUser.getUsername())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.BYPASS)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(bypassTimeTargetProperty.getPropertyName())
            .targetIdMaybe(userId);

    previousBypassTime.ifPresent(
        i -> eventBuilder.previousValueMaybe(String.valueOf(i.toEpochMilli())));
    newBypassTime.ifPresent(i -> eventBuilder.newValueMaybe(String.valueOf(i.toEpochMilli())));

    actionAuditService.send(eventBuilder.build());
  }

  @Override
  public void fireAcknowledgeTermsOfService(DbUser targetUser, Integer termsOfServiceVersion) {
    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.USER)
            .agentIdMaybe(targetUser.getUserId())
            .agentEmailMaybe(targetUser.getUsername())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.EDIT)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(AccountTargetProperty.ACKNOWLEDGED_TOS_VERSION.getPropertyName())
            .targetIdMaybe(targetUser.getUserId())
            .newValueMaybe(String.valueOf(termsOfServiceVersion))
            .build());
  }

  @Override
  public void fireSetFreeTierDollarLimitOverride(
      Long targetUserId, @Nullable Double previousDollarQuota, @Nullable Double newDollarQuota) {
    DbUser adminUser = dbUserProvider.get();
    ActionAuditEvent.Builder builder =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.ADMINISTRATOR)
            .agentIdMaybe(adminUser.getUserId())
            .agentEmailMaybe(adminUser.getUsername())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.EDIT)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(AccountTargetProperty.FREE_TIER_DOLLAR_QUOTA.getPropertyName())
            .targetIdMaybe(targetUserId);

    if (previousDollarQuota != null) {
      builder.setPreviousValueMaybe(previousDollarQuota.toString());
    }
    if (newDollarQuota != null) {
      builder.setNewValueMaybe(newDollarQuota.toString());
    }

    actionAuditService.send(builder.build());
  }
}
