package org.pmiops.workbench.actionaudit.auditors;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
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
public class ClusterAuditorImpl implements ClusterAuditor {
  private Provider<String> actionIdProvider;
  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> userProvider;

  @Autowired
  public ClusterAuditorImpl(
      @Qualifier("ACTION_ID") Provider<String> actionIdProvider,
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> userProvider) {
    this.actionIdProvider = actionIdProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.userProvider = userProvider;
  }

  @Override
  public void fireDeleteClustersInProject(String projectId, List<String> clusterNames) {
    String actionId = actionIdProvider.get();
    actionAuditService.send(
        clusterNames.stream()
            .map(
                clusterName ->
                    ActionAuditEvent.builder()
                        .timestamp(clock.millis())
                        .actionId(actionId)
                        .agentType(AgentType.ADMINISTRATOR)
                        .agentIdMaybe(userProvider.get().getUserId())
                        .agentEmailMaybe(userProvider.get().getUsername())
                        .actionType(ActionType.DELETE)
                        .newValueMaybe(clusterName)
                        .targetType(TargetType.NOTEBOOK_SERVER)
                        .targetPropertyMaybe(projectId)
                        .build())
            .collect(Collectors.toList()));
  }
}
