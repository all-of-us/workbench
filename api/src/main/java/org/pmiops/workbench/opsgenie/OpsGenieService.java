package org.pmiops.workbench.opsgenie;

import org.pmiops.workbench.model.EgressEvent;

public interface OpsGenieService {

  // Creates an Opsgenie alert for a high-egress event detected in the Workbench system.
  public void createEgressEventAlert(EgressEvent egressEvent);
}
