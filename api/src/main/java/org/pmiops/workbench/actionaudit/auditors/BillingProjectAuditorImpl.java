package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import java.time.Clock;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditEvent.Builder;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BillingProjectAuditorImpl implements BillingProjectAuditor {
  private final ActionAuditService actionAuditService;
  private final Clock clock;
  private final Provider<String> actionIdProvider;

  @Autowired
  public BillingProjectAuditorImpl(
      ActionAuditService actionAuditService,
      Clock clock,
      @Qualifier(ACTION_ID_BEAN) Provider<String> actionIdProvider) {
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.actionIdProvider = actionIdProvider;
  }

  @Override
  public void fireDeleteAction(String billingProjectName) {
    ActionAuditEvent event =
        new Builder()
            .actionId(actionIdProvider.get())
            .actionType(ActionType.DELETE)
            .agentType(AgentType.SYSTEM)
            .targetType(TargetType.BILLING_PROJECT)
            .newValueMaybe(billingProjectName)
            .timestamp(clock.millis())
            .build();

    actionAuditService.send(event);
  }
}
