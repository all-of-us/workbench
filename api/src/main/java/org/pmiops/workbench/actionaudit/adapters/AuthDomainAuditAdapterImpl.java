package org.pmiops.workbench.actionaudit.adapters;

import java.time.Clock;
import java.util.logging.Logger;
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

  private static final Logger log = Logger.getLogger(AuthDomainAuditAdapterImpl.class.getName());

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
    try {
      actionAuditService.send(
          ActionAuditEvent.builder()
              .timestamp(clock.millis())
              .agentType(AgentType.ADMINISTRATOR)
              .agentId(adminDbUserProvider.get().getUserId())
              .agentEmailMaybe(adminDbUserProvider.get().getEmail())
              .actionId(actionIdProvider.get())
              .actionType(ActionType.EDIT)
              .targetType(TargetType.ACCOUNT)
              .targetPropertyMaybe(AccountTargetProperty.IS_ENABLED.getPropertyName())
              .targetIdMaybe(targetUserId)
              .previousValueMaybe(Boolean.toString(previousEnabledValue))
              .newValueMaybe(Boolean.toString(newEnabledValue))
              .build());
    } catch (RuntimeException e) {
      actionAuditService.logRuntimeException(log, e);
    }
  }
}
