package org.pmiops.workbench.utils.mappers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.pmiops.workbench.model.AuditAction;
import org.pmiops.workbench.model.AuditAgent;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.AuditSubAction;
import org.pmiops.workbench.model.AuditSubActionHeader;
import org.pmiops.workbench.model.AuditTarget;
import org.pmiops.workbench.model.AuditTargetPropertyChange;

@Mapper(config = MapStructConfig.class)
public interface AuditLogEntryMapper {
  AuditAgent logEntryToAgent(AuditLogEntry auditLogEntry);

  default AuditSubActionHeader logEntryToSubActionHeader(AuditLogEntry auditLogEntry) {
    return new AuditSubActionHeader()
        .target(logEntryToTarget(auditLogEntry))
        .agent(logEntryToAgent(auditLogEntry))
        .actionType(auditLogEntry.getActionType());
  }

  AuditTarget logEntryToTarget(AuditLogEntry auditLogEntry);

  AuditTargetPropertyChange logEntryToTargetPropertyChange(AuditLogEntry auditLogEntry);

  default List<AuditAction> logEntriesToActions(List<AuditLogEntry> logEntries) {
    final Multimap<String, AuditLogEntry> actionIdToRows =
        Multimaps.index(logEntries, AuditLogEntry::getActionId);
    final ImmutableList.Builder<AuditAction> resultBuilder = ImmutableList.builder();
    actionIdToRows
        .asMap()
        .forEach(
            (actionId, rows) -> {
              resultBuilder.add(buildAuditAction(actionId, rows));
            });
    return resultBuilder.build();
  }

  default AuditAction buildAuditAction(String actionId, Collection<AuditLogEntry> logEntries) {
    final AuditAction result = new AuditAction().actionId(actionId);
    final Multimap<AuditSubActionHeader, AuditLogEntry> headerToLogEntries =
        Multimaps.index(logEntries, this::logEntryToSubActionHeader);
    final List<AuditSubAction> subactions =
        headerToLogEntries.asMap().entrySet().stream()
            .map(e -> buildSubAction(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    result.subActions(subactions);
    return result;
  }

  default AuditSubAction buildSubAction(
      AuditSubActionHeader header, Collection<AuditLogEntry> logEntries) {
    return new AuditSubAction()
        .header(header)
        .propertyChanges(
            logEntries.stream()
                .map(this::logEntryToTargetPropertyChange)
                .collect(Collectors.toList()));
  }
}
