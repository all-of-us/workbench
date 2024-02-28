package org.pmiops.workbench.actionaudit;

import java.util.Collection;
import java.util.Set;

public interface ActionAuditService {
  default void send(ActionAuditEvent event) {
    send(Set.of(event));
  }

  void send(Collection<ActionAuditEvent> events);
}
