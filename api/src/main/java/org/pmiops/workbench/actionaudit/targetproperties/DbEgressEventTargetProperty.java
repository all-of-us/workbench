package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.db.model.DbEgressEvent;

/** Action properties relating to a stored high-egress event within the workbench. */
public enum DbEgressEventTargetProperty implements ModelBackedTargetProperty<DbEgressEvent> {
  EGRESS_EVENT_ID(
      "egress_event_id", dbEgressEvent -> Long.toString(dbEgressEvent.getEgressEventId())),
  USER_ID(
      "user_id",
      event -> event.getUser() != null ? Long.toString(event.getUser().getUserId()) : null),
  CREATION_TIME("creation_time", event -> PropertyUtils.toStringOrNull(event.getCreationTime())),
  STATUS("status", event -> PropertyUtils.toStringOrNull(event.getStatus()));

  private final String propertyName;
  private final Function<DbEgressEvent, String> extractor;

  DbEgressEventTargetProperty(String propertyName, Function<DbEgressEvent, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @NotNull
  @Override
  public Function<DbEgressEvent, String> getExtractor() {
    return extractor;
  }
}
