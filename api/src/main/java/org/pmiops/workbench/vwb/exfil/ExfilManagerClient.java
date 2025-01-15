package org.pmiops.workbench.vwb.exfil;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Provider;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.vwb.exfil.api.EgressEventApi;
import org.pmiops.workbench.vwb.exfil.model.UpdateEgressEventStatusBody;
import org.pmiops.workbench.vwb.exfil.model.UpdateEgressEventStatusBody.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExfilManagerClient {

  private final Provider<EgressEventApi> eggresEventApi;
  private final ExfilManagerRetryHandler exfilRetryHandler;

  // Mapping of DbEgressEventStatus to StatusEnum that is allowed to update
  private static final Map<DbEgressEventStatus, StatusEnum> SUPPORTED_EGRESS_STATUS_MAPPING =
      ImmutableMap.of(DbEgressEventStatus.REMEDIATED, StatusEnum.REMEDIATED);

  @Autowired
  public ExfilManagerClient(
      Provider<EgressEventApi> egressEventApiProvider, ExfilManagerRetryHandler exfilRetryHandler) {
    this.eggresEventApi = egressEventApiProvider;
    this.exfilRetryHandler = exfilRetryHandler;
  }

  /**
   * Updates egress event status.
   *
   * @param egressEvent egress event to update
   * @param newStatus Target state
   */
  public void updateEgressEventStatus(DbEgressEvent egressEvent, DbEgressEventStatus newStatus) {
    StatusEnum statusEnum =
        Optional.ofNullable(SUPPORTED_EGRESS_STATUS_MAPPING.get(newStatus))
            .orElseThrow(() -> new BadRequestException("Invalid new status: " + newStatus));

    exfilRetryHandler.run(
        context -> {
          eggresEventApi
              .get()
              .updateEgressEventStatus(
                  new UpdateEgressEventStatusBody().status(statusEnum),
                  egressEvent.getVwbEgressEventId());
          return null;
        });
  }
}
