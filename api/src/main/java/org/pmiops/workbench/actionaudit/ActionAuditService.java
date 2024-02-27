package org.pmiops.workbench.actionaudit;

import java.util.Collection;
import java.util.logging.Logger;

public interface ActionAuditService {
  void send(ActionAuditEvent event);

  void send(Collection<ActionAuditEvent> events);

  void logRuntimeException(Logger logger, RuntimeException exception);
}
