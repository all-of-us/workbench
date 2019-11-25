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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuthDomainAuditAdapterImpl implements AuthDomainAuditAdapter {

  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> adminDbUserProvider;
  private Provider<String> actionIdProvider;

  @Autowired
  public AuthDomainAuditAdapterImpl(
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> adminDbUserProvider,
      @Qualifier("ACTION_ID") Provider<String> actionIdProvider) {
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.adminDbUserProvider = adminDbUserProvider;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireSetAccountEnabled(
      long targetUserId, boolean newEnabledValue, boolean previousEnabledValue) {
    final ActionAuditEvent event =
        new ActionAuditEvent(
            clock.millis(),
            AgentType.ADMINISTRATOR,
            adminDbUserProvider.get().getUserId(),
            adminDbUserProvider.get().getEmail(),
            actionIdProvider.get(),
            ActionType.EDIT,
            TargetType.ACCOUNT,
            AccountTargetProperty.IS_ENABLED.getPropertyName(),
            targetUserId,
            Boolean.toString(previousEnabledValue),
            Boolean.toString(newEnabledValue));
    actionAuditService.send(event);
  }
}
