package org.pmiops.workbench.utils.mappers;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.mapstruct.Mapper;
import org.pmiops.workbench.model.AuditAction;
import org.pmiops.workbench.model.AuditAgent;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.AuditSubAction;
import org.pmiops.workbench.model.AuditSubActionHeader;
import org.pmiops.workbench.model.AuditTarget;
import org.pmiops.workbench.model.AuditTargetPropertyChange;
import org.pmiops.workbench.utils.FieldValues;

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
    final List<AuditSubAction> subActions =
        headerToLogEntries.asMap().entrySet().stream()
            .map(e -> buildSubAction(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    result.subActions(subActions);
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

  default AuditLogEntry fieldValueListToAditLogEntry(FieldValueList row) {
    final AuditLogEntry entry = new AuditLogEntry();
    FieldValues.getString(row, "action_id").ifPresent(entry::setActionId);
    FieldValues.getString(row, "action_type").ifPresent(entry::setActionType);
    FieldValues.getLong(row, "agent_id").ifPresent(entry::setAgentId);
    FieldValues.getString(row, "agent_type").ifPresent(entry::setAgentType);
    FieldValues.getString(row, "agent_username").ifPresent(entry::setAgentUsername);
    FieldValues.getDateTime(row, "event_time").ifPresent(entry::setEventTime);
    FieldValues.getString(row, "new_value").ifPresent(entry::setNewValue);
    FieldValues.getString(row, "prev_value").ifPresent(entry::setPreviousValue);
    FieldValues.getLong(row, "target_id").ifPresent(entry::setTargetId);
    FieldValues.getString(row, "target_property").ifPresent(entry::setTargetProperty);
    FieldValues.getString(row, "target_type").ifPresent(entry::setTargetType);
    return entry;
  }

  default ImmutableList<AuditLogEntry> tableResultToLogEntries(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToAditLogEntry)
        .collect(ImmutableList.toImmutableList());
  }
}
