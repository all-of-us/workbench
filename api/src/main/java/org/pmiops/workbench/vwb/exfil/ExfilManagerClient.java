package org.pmiops.workbench.vwb.exfil;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Provider;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.vwb.exfil.api.EgressEventApi;
import org.pmiops.workbench.vwb.exfil.api.EgressThresholdOverrideApi;
import org.pmiops.workbench.vwb.exfil.model.CreateEgressThresholdOverrideBody;
import org.pmiops.workbench.vwb.exfil.model.UpdateEgressEventStatusBody;
import org.pmiops.workbench.vwb.exfil.model.UpdateEgressEventStatusBody.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExfilManagerClient {

  private final Provider<EgressEventApi> egressEventApi;
  private final Provider<EgressThresholdOverrideApi> egressThresholdOverrideApi;
  private final ExfilManagerRetryHandler exfilRetryHandler;

  // Mapping of DbEgressEventStatus to StatusEnum that is allowed to update
  private static final Map<DbEgressEventStatus, StatusEnum> SUPPORTED_EGRESS_STATUS_MAPPING =
      ImmutableMap.of(
          DbEgressEventStatus.VERIFIED_FALSE_POSITIVE, StatusEnum.VERIFIED_FALSE_POSITIVE);

  @Autowired
  public ExfilManagerClient(
      Provider<EgressEventApi> egressEventApiProvider,
      Provider<EgressThresholdOverrideApi> egressThresholdOverrideApiProvider,
      ExfilManagerRetryHandler exfilRetryHandler) {
    this.egressEventApi = egressEventApiProvider;
    this.egressThresholdOverrideApi = egressThresholdOverrideApiProvider;
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
          egressEventApi
              .get()
              .updateEgressEventStatus(
                  new UpdateEgressEventStatusBody().status(statusEnum),
                  egressEvent.getVwbEgressEventId());
          return null;
        });
  }

  /**
   * Creates an egress threshold override for a user and workspace in VWB Exfil Manager.
   *
   * @param username The username for the override
   * @param workspaceId The workspace ID (UUID format)
   * @param endTime Optional end time for the override. If not provided, defaults to 48 hours from
   *     creation
   * @param description Optional description of why the override was created
   */
  public void createEgressThresholdOverride(
      String username, String workspaceId, Instant endTime, String description) {
    CreateEgressThresholdOverrideBody body =
        new CreateEgressThresholdOverrideBody()
            .username(username)
            .workspaceId(UUID.fromString(workspaceId));

    if (endTime != null) {
      body.endTime(OffsetDateTime.ofInstant(endTime, ZoneOffset.UTC));
    }
    if (description != null) {
      body.description(description);
    }

    exfilRetryHandler.run(
        context -> {
          egressThresholdOverrideApi.get().createEgressThresholdOverride(body);
          return null;
        });
  }
}
