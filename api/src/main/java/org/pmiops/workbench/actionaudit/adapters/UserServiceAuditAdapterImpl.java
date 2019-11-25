package org.pmiops.workbench.actionaudit.adapters;

import com.google.common.collect.Streams;
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
public class UserServiceAuditAdapterImpl implements UserServiceAuditAdapter {

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
      DbUser targetUser, DataAccessLevel dataAccessLevel, DataAccessLevel previousDataAccessLevel) {
    try {
      ActionAuditEvent event =
          new ActionAuditEvent(
              clock.millis(),
              AgentType.ADMINISTRATOR,
              dbUserProvider.get().getUserId(),
              dbUserProvider.get().getEmail(),
              actionIdProvider.get(),
              ActionType.EDIT,
              TargetType.ACCOUNT,
              AccountTargetProperty.REGISTRATION_STATUS.getPropertyName(),
              targetUser.getUserId(),
              previousDataAccessLevel.toString(),
              dataAccessLevel.toString());
      actionAuditService.send(event);
    } catch (RuntimeException e) {
      actionAuditService.logRuntimeException(log, e);
    }
  }

  @Override
  public void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Optional<Instant> bypassTime) {
    try {
      actionAuditService.send(new ActionAuditEvent(
          clock.millis(),
          AgentType.ADMINISTRATOR,
          dbUserProvider.get().getUserId(),
          dbUserProvider.get().getEmail(),
          actionIdProvider.get(),
          ActionType.BYPASS,
          TargetType.ACCOUNT,
          bypassTimeTargetProperty.getPropertyName(),
          userId,
          null,
          bypassTime
            .map(Instant::toEpochMilli)
            .map(String::valueOf)
            .orElse(null)));
    } catch (RuntimeException e) {
      actionAuditService.logRuntimeException(log, e);
    }
  }
}
