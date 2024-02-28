package org.pmiops.workbench.actionaudit;

import java.util.Collection;
import java.util.List;

public interface ActionAuditService {
  default void send(ActionAuditEvent event) {
    send(List.of(event));
  }

  void send(Collection<ActionAuditEvent> events);
}
