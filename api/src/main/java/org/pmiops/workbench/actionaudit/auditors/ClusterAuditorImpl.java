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
import org.springframework.stereotype.Service;

@Service
public class ClusterAuditorImpl implements ClusterAuditor {
  private Provider<String> actionIdProvider;
  private ActionAuditService actionAuditService;
  private Clock clock;
  private Provider<DbUser> adminDbUserProvider;

  @Autowired
  public ClusterAuditorImpl(
      Provider<String> actionIdProvider,
      ActionAuditService actionAuditService,
      Clock clock,
      Provider<DbUser> adminDbUserProvider) {
    this.actionIdProvider = actionIdProvider;
    this.actionAuditService = actionAuditService;
    this.clock = clock;
    this.adminDbUserProvider = adminDbUserProvider;
  }

  @Override
  public void fireDeleteClustersInProject(String projectId, List<String> clusterNames) {
    actionAuditService.send(
        clusterNames.stream()
            .map(
                clusterName ->
                    ActionAuditEvent.builder()
                        .timestamp(clock.millis())
                        .agentType(AgentType.ADMINISTRATOR)
                        .agentId(adminDbUserProvider.get().getUserId())
                        .agentEmailMaybe(adminDbUserProvider.get().getUsername())
                        .actionType(ActionType.DELETE)
                        .newValueMaybe(clusterName)
                        .targetType(TargetType.NOTEBOOK_SERVER)
                        .targetPropertyMaybe(projectId)
                        .build())
            .collect(Collectors.toList()));
  }
}
