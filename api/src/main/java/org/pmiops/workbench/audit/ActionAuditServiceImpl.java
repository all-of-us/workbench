package org.pmiops.workbench.audit;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditServiceImpl implements ActionAuditService {
  private static final Logger logger = Logger.getLogger(ActionAuditServiceImpl.class.getName());
  private final Logging logging;

  @Autowired
  public ActionAuditServiceImpl(Logging logging) {
    this.logging = logging;
  }

  @Override
  public void send(ActionAuditEvent event) {
    send(Collections.singleton(event));
  }

  @Override
  public void send(Collection<ActionAuditEvent> events) {
    try {
      ImmutableList<LogEntry> logEntries =
          events.stream()
              .map(ActionAuditEvent::toLogEntry)
              .collect(ImmutableList.toImmutableList());
      logging.write(logEntries);
    } catch (RuntimeException e) {
      logger.log(
          Level.SEVERE, e, () -> "Exception encountered writing log entries to Cloud Logging.");
    }
  }
}
