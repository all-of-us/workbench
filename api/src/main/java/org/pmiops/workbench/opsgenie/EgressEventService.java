package org.pmiops.workbench.opsgenie;

import org.pmiops.workbench.model.EgressEvent;

public interface EgressEventService {
  void handleEvent(EgressEvent egressEvent);
}
