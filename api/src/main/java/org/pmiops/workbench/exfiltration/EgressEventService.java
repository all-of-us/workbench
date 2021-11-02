package org.pmiops.workbench.exfiltration;

import org.pmiops.workbench.model.SumologicEgressEvent;

public interface EgressEventService {
  void handleEvent(SumologicEgressEvent egressEvent);
}
