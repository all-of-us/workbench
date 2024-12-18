package org.pmiops.workbench.exfiltration;

import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.VwbEgressEventRequest;

public interface EgressEventService {
  void handleEvent(SumologicEgressEvent egressEvent);

  void handleVwbEvent(VwbEgressEventRequest egressEvent);
}
