package org.pmiops.workbench.utils.mappers;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.mapstruct.Mapper;
import org.pmiops.workbench.model.AuditAction;
import org.pmiops.workbench.model.AuditAgent;
import org.pmiops.workbench.model.AuditEventBundle;
import org.pmiops.workbench.model.AuditEventBundleHeader;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.AuditTarget;
import org.pmiops.workbench.model.AuditTargetPropertyChange;
import org.pmiops.workbench.utils.FieldValues;

@Mapper(config = MapStructConfig.class)
public interface AuditLogEntryMapper {
  AuditAgent logEntryToAgent(AuditLogEntry auditLogEntry);

  default AuditEventBundleHeader logEntryToEventBundleHeader(AuditLogEntry auditLogEntry) {
    return new AuditEventBundleHeader()
        .target(logEntryToTarget(auditLogEntry))
        .agent(logEntryToAgent(auditLogEntry))
        .actionType(auditLogEntry.getActionType());
  }

  AuditTarget logEntryToTarget(AuditLogEntry auditLogEntry);

  /**
   * Build an AuditTargetPropertyChange object from the relevant fields in the AuditLogEntry, unless
   * all of those are null, in which case return an empty optional
   *
   * @param auditLogEntry
   * @return
   */
  default Optional<AuditTargetPropertyChange> logEntryToTargetPropertyChange(
      AuditLogEntry auditLogEntry) {
    if ((Strings.isNullOrEmpty(auditLogEntry.getTargetProperty())
            && Strings.isNullOrEmpty(auditLogEntry.getPreviousValue()))
        && Strings.isNullOrEmpty(auditLogEntry.getNewValue())) {
      return Optional.empty();
    } else {
      return Optional.of(
          new AuditTargetPropertyChange()
              .targetProperty(auditLogEntry.getTargetProperty())
              .previousValue(auditLogEntry.getPreviousValue())
              .newValue(auditLogEntry.getNewValue()));
    }
  }

  default List<AuditAction> logEntriesToActions(List<AuditLogEntry> logEntries) {
    final Multimap<String, AuditLogEntry> actionIdToRows =
        Multimaps.index(logEntries, AuditLogEntry::getActionId);
    return actionIdToRows.asMap().values().stream()
        .map(this::buildAuditAction)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * @param logEntries Collection of AuditLogEntry objectsthat have a common Action ID, which should
   *     be the same event time, given the unofficial but surprisingly harshly enforced non-schema
   *     schama.
   */
  default AuditAction buildAuditAction(Collection<AuditLogEntry> logEntries) {
    final AuditLogEntry firstEntry =
        logEntries.stream()
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("logEntries collection must not be empty"));

    final AuditAction result =
        new AuditAction().actionId(firstEntry.getActionId()).actionTime(firstEntry.getEventTime());

    final Multimap<AuditEventBundleHeader, AuditLogEntry> headerToLogEntries =
        Multimaps.index(logEntries, this::logEntryToEventBundleHeader);
    final List<AuditEventBundle> eventBundles =
        headerToLogEntries.asMap().entrySet().stream()
            .map(e -> buildEventBundle(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

    return result.eventBundles(eventBundles);
  }

  default AuditEventBundle buildEventBundle(
      AuditEventBundleHeader header, Collection<AuditLogEntry> logEntries) {
    return new AuditEventBundle()
        .header(header)
        .propertyChanges(
            logEntries.stream()
                .map(this::logEntryToTargetPropertyChange)
                .flatMap(Streams::stream)
                .collect(Collectors.toList()));
  }

  default AuditLogEntry fieldValueListToAuditLogEntry(FieldValueList row) {
    final AuditLogEntry entry = new AuditLogEntry();
    FieldValues.getString(row, "action_id").ifPresent(entry::setActionId);
    FieldValues.getString(row, "action_type").ifPresent(entry::setActionType);
    FieldValues.getLong(row, "agent_id").ifPresent(entry::setAgentId);
    FieldValues.getString(row, "agent_type").ifPresent(entry::setAgentType);
    FieldValues.getString(row, "agent_username").ifPresent(entry::setAgentUsername);
    FieldValues.getDateTime(row, "event_time")
        .ifPresent(odt -> entry.setEventTime(odt.toInstant().toEpochMilli()));
    FieldValues.getString(row, "new_value").ifPresent(entry::setNewValue);
    FieldValues.getString(row, "prev_value").ifPresent(entry::setPreviousValue);
    FieldValues.getLong(row, "target_id").ifPresent(entry::setTargetId);
    FieldValues.getString(row, "target_property").ifPresent(entry::setTargetProperty);
    FieldValues.getString(row, "target_type").ifPresent(entry::setTargetType);
    return entry;
  }

  default ImmutableList<AuditLogEntry> tableResultToLogEntries(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToAuditLogEntry)
        .collect(ImmutableList.toImmutableList());
  }
}
