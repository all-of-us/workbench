package org.pmiops.workbench.audit;

import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Service;

public interface ActionAuditService {
  void send(ActionAuditEvent event);

  void send(Collection<ActionAuditEvent> events);

  static String newActionId() {
    return UUID.randomUUID().toString();
  }
}
