package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.model.StatusAlertLocation;
import org.pmiops.workbench.statusalerts.StatusAlertMapperImpl;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class StatusAlertControllerTest {
  private static final Instant NOW = Instant.now();
  private final String STATUS_ALERT_INITIAL_TITLE = "Hello World";
  private final String STATUS_ALERT_INITIAL_DESCRIPTION = "Status alert description";
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired private StatusAlertController statusAlertController;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, StatusAlertController.class, StatusAlertMapperImpl.class})
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    StatusAlert initialStatusAlert =
        new StatusAlert()
            .title(STATUS_ALERT_INITIAL_TITLE)
            .message(STATUS_ALERT_INITIAL_DESCRIPTION)
            .alertLocation(StatusAlertLocation.AFTER_LOGIN);
    statusAlertController.postStatusAlert(initialStatusAlert);
  }

  @Test
  public void testGetStatusAlerts() {
    List<StatusAlert> statusAlerts = statusAlertController.getStatusAlerts().getBody();
    assertThat(statusAlerts).isNotEmpty();
    StatusAlert statusAlert = statusAlerts.get(0);
    assertThat(statusAlert.getTitle()).matches(STATUS_ALERT_INITIAL_TITLE);
    assertThat(statusAlert.getMessage()).matches(STATUS_ALERT_INITIAL_DESCRIPTION);
    assertThat(statusAlert.getAlertLocation()).isEqualTo(StatusAlertLocation.AFTER_LOGIN);
  }

  @ParameterizedTest
  @EnumSource(StatusAlertLocation.class)
  public void testPostStatusAlert(StatusAlertLocation location) {
    String updatedStatusAlertTitle = "Title 2";
    String updatedStatusAlertDescription = "Description 2";
    String updatedStatusAlertLink = "This has a link";

    StatusAlert statusAlert =
        new StatusAlert()
            .title(updatedStatusAlertTitle)
            .message(updatedStatusAlertDescription)
            .link(updatedStatusAlertLink)
            .alertLocation(location);

    statusAlertController.postStatusAlert(statusAlert);
    List<StatusAlert> statusAlerts = statusAlertController.getStatusAlerts().getBody();
    assertThat(statusAlerts).isNotEmpty();
    StatusAlert updatedStatusAlert = statusAlerts.get(0);
    assertThat(updatedStatusAlert.getTitle())
        .matches(
            location.equals(StatusAlertLocation.AFTER_LOGIN)
                ? updatedStatusAlertTitle
                : "Scheduled Downtime Notice for the Researcher Workbench");
    assertThat(updatedStatusAlert.getMessage()).matches(updatedStatusAlertDescription);
    assertThat(updatedStatusAlert.getLink()).matches(updatedStatusAlertLink);
    assertThat(updatedStatusAlert.getAlertLocation()).isEqualTo(location);
  }
}
