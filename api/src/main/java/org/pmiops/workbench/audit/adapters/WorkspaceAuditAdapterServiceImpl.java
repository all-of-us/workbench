package org.pmiops.workbench.audit.adapters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.audit.ActionAuditService;
import org.pmiops.workbench.audit.ActionType;
import org.pmiops.workbench.audit.AgentType;
import org.pmiops.workbench.audit.AuditableEvent;
import org.pmiops.workbench.audit.TargetType;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.workspaces.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuditAdapterServiceImpl implements WorkspaceAuditAdapterService {

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
    final String actionId = ActionAuditService.newActionId();
    final long userId = userProvider.get().getUserId();
    final String userEmail = userProvider.get().getEmail();
    final Map<String, String> propertyValues = buildPropertyStringMap(createdWorkspace);
    List<AuditableEvent> events =
        propertyValues.entrySet().stream()
            .map(
                entry ->
                    new AuditableEvent.Builder()
                        .setActionId(actionId)
                        .setAgentEmailMaybe(Optional.of(userEmail))
                        .setActionType(ActionType.CREATE)
                        .setAgentType(AgentType.USER)
                        .setAgentId(userId)
                        .setTargetType(TargetType.WORKSPACE)
                        .setTargetPropertyMaybe(Optional.of(entry.getKey()))
                        .setTargetIdMaybe(Optional.of(dbWorkspaceId))
                        .setPreviousValueMaybe(Optional.empty())
                        .setNewValueMaybe(Optional.of(entry.getValue()))
                        .build())
            .collect(Collectors.toList());
    actionAuditService.send(events);
  }

  // Because the deleteWorkspace() method operates at the DB level, this method
  // has a different signature than for the create action. This makes keeping the properties
  // consistent across actions somewhat challenging.
  @Override
  public void fireDeleteAction(org.pmiops.workbench.db.model.Workspace dbWorkspace) {
    final Workspace workspace = WorkspaceMapper.toApiWorkspace(dbWorkspace);
    final String actionId = ActionAuditService.newActionId();
    final long userId = userProvider.get().getUserId();
    final String userEmail = userProvider.get().getEmail();
    final AuditableEvent event = new AuditableEvent.Builder()
        .setActionId(actionId)
        .setAgentEmailMaybe(Optional.of(userEmail))
        .setActionType(ActionType.DELETE)
        .setAgentType(AgentType.USER)
        .setAgentId(userId)
        .setTargetType(TargetType.WORKSPACE)
        .setTargetIdMaybe(Optional.of(dbWorkspace.getWorkspaceId()))
        .build();

    actionAuditService.send(event);
  }

  private Map<String, String> buildPropertyStringMap(Workspace workspace) {
    ImmutableMap.Builder<String, String> propsBuilder = new ImmutableMap.Builder<>();
    final ResearchPurpose researchPurpose = workspace.getResearchPurpose(); // required field
    insertIfNotNull(propsBuilder, "intended_study", researchPurpose.getIntendedStudy());
    insertIfNotNull(propsBuilder, "additional_notes", researchPurpose.getAdditionalNotes());
    insertIfNotNull(propsBuilder, "anticipated_findings", researchPurpose.getAnticipatedFindings());
    insertIfNotNull(propsBuilder, "disease_of_focus", researchPurpose.getDiseaseOfFocus());
    insertIfNotNull(propsBuilder, "reason_for_aou", researchPurpose.getReasonForAllOfUs());
    insertIfNotNull(propsBuilder, "creator", workspace.getCreator());
    Optional.ofNullable(workspace.getNamespace())
        .ifPresent(ns -> propsBuilder.put("namespace", ns));
    Optional.ofNullable(workspace.getId()).ifPresent(id -> propsBuilder.put("firecloud_name", id));
    Optional.ofNullable(workspace.getName()).ifPresent(n -> propsBuilder.put("name", n));
    return propsBuilder.build();
  }

  private void insertIfNotNull(
      ImmutableMap.Builder<String, String> mapBuilder, String key, String value) {
    Optional.ofNullable(value).ifPresent(v -> mapBuilder.put(key, v));
  }
}
