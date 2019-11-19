package org.pmiops.workbench.actionaudit.adapters;

import java.time.Clock;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserServiceAuditAdapterImpl implements UserServiceAuditAdapter {

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
      DataAccessLevel dataAccessLevel, DataAccessLevel previousDataAccessLevel) {
    // Populate the Agent ID and Agent Email with the target user's info,
    // since the SYSTEM agent doesn't have those and we need them anyway.
    ActionAuditEvent event =
        new ActionAuditEvent(
            clock.millis(),
            AgentType.SYSTEM,
            dbUserProvider.get().getUserId(),
            dbUserProvider.get().getEmail(),
            actionIdProvider.get(),
            ActionType.EDIT,
            TargetType.ACCOUNT,
            AccountTargetProperty.IS_ENABLED.toString(),
            dbUserProvider.get().getUserId(),
            previousDataAccessLevel.toString(),
            dataAccessLevel.toString());
    actionAuditService.send(event);
  }
}
