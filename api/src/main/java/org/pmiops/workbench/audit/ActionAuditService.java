package org.pmiops.workbench.audit;

import java.util.Collection;
import org.elasticsearch.common.UUIDs;

public interface ActionAuditService {
  void send(Collection<AuditableEvent> events);

  static String newActionId() {
    return UUIDs.legacyBase64UUID();
  }
}
