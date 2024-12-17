package org.pmiops.workbench.api;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_SUMOLOGIC_SERVICE_QUALIFIER;
import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_VWB_SERVICE_QUALIFIER;

import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.model.ProcessEgressEventRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskEgressController implements CloudTaskEgressApiDelegate {

  private final EgressRemediationService sumologicEgressRemediationService;
  private final EgressRemediationService vwbEgressRemediationService;

  public CloudTaskEgressController(
      @Qualifier(EGRESS_SUMOLOGIC_SERVICE_QUALIFIER)
          EgressRemediationService sumologicEgressRemediationService,
      @Qualifier(EGRESS_VWB_SERVICE_QUALIFIER) EgressRemediationService vwbEgressRemediationService,
      EgressEventDao egressEventDao) {
    this.sumologicEgressRemediationService = sumologicEgressRemediationService;
    this.vwbEgressRemediationService = vwbEgressRemediationService;
  }

  @Override
  public ResponseEntity<Void> processEgressEvent(ProcessEgressEventRequest request) {
    if (request.isIsVwbEgressEvent()) {
      vwbEgressRemediationService.remediateEgressEvent(request.getEventId());
    } else {
      sumologicEgressRemediationService.remediateEgressEvent(request.getEventId());
    }
    return ResponseEntity.ok().build();
  }
}
