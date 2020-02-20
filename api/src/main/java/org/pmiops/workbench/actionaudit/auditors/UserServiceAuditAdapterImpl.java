package org.pmiops.workbench.actionaudit.auditors;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserServiceAuditAdapterImpl implements UserServiceAuditor {

  private static final Logger log = Logger.getLogger(UserServiceAuditAdapterImpl.class.getName());

  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private Provider<DbUser> dbUserProvider;
  private Provider<String> actionIdProvider;

  @Autowired
  public UserServiceAuditAdapterImpl(
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
  public void fireUpdateDataAccessAction(
      DbUser targetUser,
      DataAccessLevel dataAccessLevel,
      DataAccessLevel previousDataAccessLevel,
      AgentType agentType) {
    // In the event of an offline cronjob, the request may have been made anonymously and there is
    // no user identity. This should only happen for system agent driven audit events.
    Optional<DbUser> agent = Optional.ofNullable(dbUserProvider.get());
    if (agent.isPresent() && AgentType.SYSTEM == agentType) {
      log.warning(
          String.format(
              "found unexpected request scope user '%s' for SYSTEM agent audit action",
              agent.get().getUsername()));
    } else if (!agent.isPresent() && AgentType.SYSTEM != agentType) {
      log.warning(
          String.format("missing request scope user for audit action agent type of %s", agentType));
    }

    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(agentType)
            .agentId(agent.map(DbUser::getUserId).orElse(0L))
            .agentEmailMaybe(agent.map(DbUser::getUsername).orElse(null))
            .actionId(actionIdProvider.get())
            .actionType(ActionType.EDIT)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(AccountTargetProperty.REGISTRATION_STATUS.getPropertyName())
            .targetIdMaybe(targetUser.getUserId())
            .previousValueMaybe(previousDataAccessLevel.toString())
            .newValueMaybe(dataAccessLevel.toString())
            .build());
  }

  @Override
  public void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Optional<Instant> bypassTime) {
    DbUser adminUser = dbUserProvider.get();
    ActionAuditEvent.Builder eventBuilder =
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.ADMINISTRATOR)
            .agentId(adminUser.getUserId())
            .agentEmailMaybe(adminUser.getUsername())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.BYPASS)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(bypassTimeTargetProperty.getPropertyName())
            .targetIdMaybe(userId);
    bypassTime.ifPresent(i -> eventBuilder.newValueMaybe(String.valueOf(i.toEpochMilli())));

    actionAuditService.send(eventBuilder.build());
  }

  @Override
  public void fireAcknowledgeTermsOfService(DbUser targetUser, Integer termsOfServiceVersion) {
    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.USER)
            .agentId(targetUser.getUserId())
            .agentEmailMaybe(targetUser.getUsername())
            .actionId(actionIdProvider.get())
            .actionType(ActionType.EDIT)
            .targetType(TargetType.ACCOUNT)
            .targetPropertyMaybe(AccountTargetProperty.ACKNOWLEDGED_TOS_VERSION.getPropertyName())
            .targetIdMaybe(targetUser.getUserId())
            .newValueMaybe(String.valueOf(termsOfServiceVersion))
            .build());
  }
}
