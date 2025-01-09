package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbEgressEventMapperTest {

  @Autowired private VwbEgressEventMapper mapper;

  @TestConfiguration
  @Import({VwbEgressEventMapperImpl.class, CommonMappers.class, FakeClockConfiguration.class})
  static class Configuration {}

  @Test
  public void toApiEvent() {
    DbUser user = new DbUser();
    user.setUserId(13L);
    user.setUsername("asdf@fake-research-aou.org");

    Instant created = FakeClockConfiguration.NOW.toInstant();
    Instant modified = created.plus(Duration.ofMinutes(5L));
    Timestamp timeStart = Timestamp.from(created.minus(Duration.ofHours(1L)));
    long timeWindowStartMilli = timeStart.getTime();
    long timeWindowDurationSeconds = Duration.ofHours(1L).getSeconds();
    double egressMB = 201.0;
    double egressMib = 200.0;

    assertThat(
            mapper.toApiEvent(
                new DbEgressEvent()
                    .setEgressEventId(7L)
                    .setUser(user)
                    .setVwbWorkspaceId("ns")
                    .setGcpProjectId("proj")
                    .setCreationTime(Timestamp.from(created))
                    .setStatus(DbEgressEventStatus.PENDING)
                    .setLastModifiedTime(Timestamp.from(modified))
                    .setEgressMegabytes((float) egressMB)
                    .setEgressWindowSeconds(timeWindowDurationSeconds)
                    .setTimeWindowStart(timeStart)))
        .isEqualTo(
            new EgressEvent()
                .egressEventId("7")
                .sourceUserEmail(user.getUsername())
                .sourceWorkspaceNamespace("ns")
                .sourceGoogleProject("proj")
                .creationTime("2000-01-01T00:00:00Z")
                .status(EgressEventStatus.PENDING)
                .egressMegabytes(egressMB)
                .egressWindowSeconds(BigDecimal.valueOf(timeWindowDurationSeconds))
                .timeWindowStartEpochMillis(timeWindowStartMilli)
                .timeWindowEndEpochMillis(timeWindowStartMilli + timeWindowDurationSeconds * 1000));
  }
}
