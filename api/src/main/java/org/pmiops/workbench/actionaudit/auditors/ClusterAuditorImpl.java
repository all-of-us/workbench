package org.pmiops.workbench.actionaudit.auditors;

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

import javax.inject.Provider;
import java.time.Clock;

@Service
public class ClusterAuditorImpl implements ClusterAuditor {
  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> adminDbUserProvider;

  @Autowired
  public ClusterAuditorImpl(
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> adminDbUserProvider) {
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.adminDbUserProvider = adminDbUserProvider;
  }

  @Override
  public void fireDeleteClustersInProject(String projectId) {
    actionAuditService.send(
        ActionAuditEvent.builder()
            .timestamp(clock.millis())
            .agentType(AgentType.ADMINISTRATOR)
            .agentId(adminDbUserProvider.get().getUserId())
            .agentEmailMaybe(adminDbUserProvider.get().getUsername())
            .actionType(ActionType.DELETE)
            .targetType(TargetType.NOTEBOOK_SERVER)
            .targetPropertyMaybe(projectId)
            .build());
  }
}
