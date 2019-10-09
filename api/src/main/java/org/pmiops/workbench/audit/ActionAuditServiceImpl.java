package org.pmiops.workbench.audit;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditServiceImpl implements ActionAuditService {
  private final Logging logging;

  @Autowired
  public ActionAuditServiceImpl(Logging logging) {
    this.logging = logging;
  }

  @Override
  public void send(AuditableEvent event) {
    send(Collections.singleton(event));
  }

  @Override
  public void send(Collection<AuditableEvent> events) {
    ImmutableList<LogEntry> logEntries =
        events.stream().map(AuditableEvent::toLogEntry).collect(ImmutableList.toImmutableList());
    logging.write(logEntries);
  }
}
