package org.pmiops.workbench.audit;

import java.util.Collection;

public interface ActionAuditService {
  void send(ActionAuditEvent event);

  void send(Collection<ActionAuditEvent> events);
}
