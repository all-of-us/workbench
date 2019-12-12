package org.pmiops.workbench.actionaudit.auditors;

import java.time.Clock;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.actionaudit.targetproperties.AccountTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.values.AccountDisabledStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuthDomainAuditorImpl implements AuthDomainAuditor {

  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> adminDbUserProvider;
  private Provider<String> actionIdProvider;

  @Autowired
  public AuthDomainAuditorImpl(
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
  public void fireSetAccountDisabledStatus(
      long targetUserId, boolean newDisabledValue, boolean oldDisabledValue) {
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
            .previousValueMaybe(
                AccountDisabledStatus.fromDisabledBoolean(oldDisabledValue).getValueName())
            .newValueMaybe(
                AccountDisabledStatus.fromDisabledBoolean(newDisabledValue).getValueName())
            .build());
  }
}
