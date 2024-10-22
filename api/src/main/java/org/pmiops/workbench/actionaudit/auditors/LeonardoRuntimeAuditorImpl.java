package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import jakarta.inject.Provider;
import java.time.Clock;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoRuntimeAuditorImpl implements LeonardoRuntimeAuditor {
  private Provider<String> actionIdProvider;
  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> userProvider;

  @Autowired
  public LeonardoRuntimeAuditorImpl(
      @Qualifier(ACTION_ID_BEAN) Provider<String> actionIdProvider,
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> userProvider) {
    this.actionIdProvider = actionIdProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.userProvider = userProvider;
  }

  @Override
  public void fireDeleteRuntime(String googleProjectId, String runtimeName) {
    String actionId = actionIdProvider.get();
    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .actionId(actionId)
            .agentType(AgentType.ADMINISTRATOR)
            .agentIdMaybe(userProvider.get().getUserId())
            .agentEmailMaybe(userProvider.get().getUsername())
            .actionType(ActionType.DELETE)
            .newValueMaybe(runtimeName)
            .targetType(TargetType.NOTEBOOK_SERVER)
            .targetPropertyMaybe(googleProjectId)
            .build());
  }
}
