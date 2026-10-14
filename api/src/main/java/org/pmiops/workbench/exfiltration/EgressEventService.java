package org.pmiops.workbench.exfiltration;

import org.pmiops.workbench.model.VwbEgressEventRequest;

public interface EgressEventService {

  void handleVwbEvent(VwbEgressEventRequest egressEvent);
}
