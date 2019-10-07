package org.pmiops.workbench.audit;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;

public class ActionAuditServiceImpl implements ActionAuditService {
  private final Logging logging;

  @Autowired
  public ActionAuditServiceImpl() {
    this.logging = LoggingOptions.getDefaultInstance().getService();
  }

  @Override
  public void send(Collection<AuditableEvent> events) {
    ImmutableList<LogEntry> logEntries = events.stream()
        .map(AuditableEvent::toLogEntry)
        .collect(ImmutableList.toImmutableList());
    logging.write(logEntries);
  }
}
