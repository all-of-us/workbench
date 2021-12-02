package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class EgressEventMapperTest {

  @Autowired private EgressEventMapper mapper;

  @TestConfiguration
  @Import({EgressEventMapperImpl.class, CommonMappers.class, FakeClockConfiguration.class})
  static class Configuration {}

  @Test
  public void toApiEvent() {
    DbUser user = new DbUser();
    user.setUserId(13L);
    user.setUsername("asdf@fake-research-aou.org");

    Instant created = FakeClockConfiguration.NOW.toInstant();
    Instant modified = created.plus(Duration.ofMinutes(5L));
    assertThat(
            mapper.toApiEvent(
                new DbEgressEvent()
                    .setEgressEventId(7L)
                    .setUser(user)
                    .setWorkspace(
                        new DbWorkspace().setWorkspaceNamespace("ns").setGoogleProject("proj"))
                    .setCreationTime(Timestamp.from(created))
                    .setStatus(DbEgressEventStatus.PENDING)
                    .setLastModifiedTime(Timestamp.from(modified))
                    .setEgressMegabytes((float) 201.0)
                    .setEgressWindowSeconds(3600L)
                    .setSumologicEvent(
                        new Gson()
                            .toJson(
                                new SumologicEgressEvent()
                                    .egressMib(200.0)
                                    .timeWindowStart(
                                        created.minus(Duration.ofHours(1L)).toEpochMilli())
                                    .timeWindowDuration(Duration.ofHours(1L).toMillis())
                                    .vmPrefix(user.getRuntimeName())))))
        .isEqualTo(
            new EgressEvent()
                .egressEventId("7")
                .sourceUserEmail(user.getUsername())
                .sourceWorkspaceNamespace("ns")
                .sourceGoogleProject("proj")
                .creationTime(Timestamp.from(created).toString())
                .status(EgressEventStatus.PENDING)
                .egressMegabytes(201.0)
                .egressWindowSeconds(BigDecimal.valueOf(3600L)));
  }

  @Test
  public void toApiEvent_sparse() {
    Instant created = FakeClockConfiguration.NOW.toInstant();
    Instant modified = created.plus(Duration.ofMinutes(5L));
    assertThat(
            mapper.toApiEvent(
                new DbEgressEvent()
                    .setEgressEventId(7L)
                    .setCreationTime(Timestamp.from(created))
                    .setStatus(DbEgressEventStatus.PENDING)
                    .setLastModifiedTime(Timestamp.from(modified))))
        .isEqualTo(
            new EgressEvent()
                .egressEventId("7")
                .creationTime(Timestamp.from(created).toString())
                .status(EgressEventStatus.PENDING));
  }
}
