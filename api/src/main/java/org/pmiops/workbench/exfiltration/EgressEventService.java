package org.pmiops.workbench.exfiltration;

import org.pmiops.workbench.model.EgressEvent;

public interface EgressEventService {
  void handleEvent(EgressEvent egressEvent);
}
