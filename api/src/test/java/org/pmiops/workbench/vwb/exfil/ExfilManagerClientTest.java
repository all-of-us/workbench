package org.pmiops.workbench.vwb.exfil;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.vwb.exfil.api.EgressEventApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import({ExfilManagerClient.class, ExfilManagerRetryHandler.class})
@SpringJUnitConfig
public class ExfilManagerClientTest {
  @Autowired private ExfilManagerClient exfilManagerClient;

  @MockBean ExfilManagerRetryHandler exfilManagerRetryHandler;

  private final DbEgressEvent dbEgressEvent = new DbEgressEvent();

  @Mock EgressEventApi mockEgressEventApi;

  @Test
  public void testUpdateStatus_notUupported() {
    assertThrows(
        BadRequestException.class,
        () ->
            exfilManagerClient.updateEgressEventStatus(dbEgressEvent, DbEgressEventStatus.PENDING));
  }
}
