package org.pmiops.workbench.audit.adapters;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.audit.ActionAuditEvent;
import org.pmiops.workbench.audit.ActionAuditService;
import org.pmiops.workbench.audit.ActionType;
import org.pmiops.workbench.audit.AgentType;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.audit.targetproperties.WorkspaceTargetProperty;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuditAdapterServiceImpl implements WorkspaceAuditAdapterService {

  private static final Logger logger =
      Logger.getLogger(WorkspaceAuditAdapterServiceImpl.class.getName());

  private Provider<User> userProvider;
  private ActionAuditService actionAuditService;

  @Autowired
  public WorkspaceAuditAdapterServiceImpl(
      Provider<User> userProvider, ActionAuditService actionAuditService) {
    this.userProvider = userProvider;
    this.actionAuditService = actionAuditService;
  }

  @Override
  public void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId) {
    try {
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final Map<String, String> propertyValues =
          WorkspaceTargetProperty.getPropertyValuesByName(createdWorkspace);
      // omit the previous value column
      ImmutableList<ActionAuditEvent> events =
          propertyValues.entrySet().stream()
              .map(
                  entry ->
                      new ActionAuditEvent.Builder()
                          .setActionId(actionId)
                          .setAgentEmail(userEmail)
                          .setActionType(ActionType.CREATE)
                          .setAgentType(AgentType.USER)
                          .setAgentId(userId)
                          .setTargetType(TargetType.WORKSPACE)
                          .setTargetProperty(entry.getKey())
                          .setTargetId(dbWorkspaceId)
                          .setNewValue(entry.getValue())
                          .build())
              .collect(ImmutableList.toImmutableList());
      actionAuditService.send(events);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, e, () -> "Exception encountered during audit.");
  }

  // Because the deleteWorkspace() method operates at the DB level, this method
  // has a different signature than for the create action. This makes keeping the properties
  // consistent across actions somewhat challenging.
  @Override
  public void fireDeleteAction(DbWorkspace dbWorkspace) {
    try {
      final String actionId = ActionAuditService.newActionId();
      final long userId = userProvider.get().getUserId();
      final String userEmail = userProvider.get().getEmail();
      final ActionAuditEvent event =
          new ActionAuditEvent.Builder()
              .setActionId(actionId)
              .setAgentEmail(userEmail)
              .setActionType(ActionType.DELETE)
              .setAgentType(AgentType.USER)
              .setAgentId(userId)
              .setTargetType(TargetType.WORKSPACE)
              .setTargetId(dbWorkspace.getWorkspaceId())
              .build();

      actionAuditService.send(event);
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }
}
