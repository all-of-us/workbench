package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class EgressBypassWindowMapperTest {

  @Autowired private EgressBypassWindowMapper mapper;

  @TestConfiguration
  @Import({EgressBypassWindowMapperImpl.class, CommonMappers.class, FakeClockConfiguration.class})
  static class Configuration {}

  @Test
  public void toApiEventBypassWindwo() {
    Timestamp endTime =
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(2, ChronoUnit.DAYS));

    assertThat(
            mapper.toApiEgressBypassWindow(
                new DbUserEgressBypassWindow()
                    .setUserId(123L)
                    .setStartTime(FakeClockConfiguration.NOW)
                    .setEndTime(endTime)
                    .setDescription("test mapper")))
        .isEqualTo(
            new EgressBypassWindow()
                .startTime(FakeClockConfiguration.NOW.getTime())
                .endTime(endTime.getTime())
                .description("test mapper"));
  }
}
