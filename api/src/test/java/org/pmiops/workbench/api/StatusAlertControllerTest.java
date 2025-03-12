package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.model.StatusAlertLocation;
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
  @Import({FakeClockConfiguration.class, StatusAlertController.class})
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
  public void testGetStatusAlert() {
    StatusAlert statusAlert = statusAlertController.getStatusAlert().getBody();
    assertThat(statusAlert.getTitle()).matches(STATUS_ALERT_INITIAL_TITLE);
    assertThat(statusAlert.getMessage()).matches(STATUS_ALERT_INITIAL_DESCRIPTION);
    assertThat(statusAlert.getAlertLocation()).isEqualTo(StatusAlertLocation.AFTER_LOGIN);
  }

  @ParameterizedTest
  @EnumSource(StatusAlertLocation.class) // This will provide both BEFORE_LOGIN and AFTER_LOGIN
  public void testPostStatusAlert(StatusAlertLocation location) {
    String updatedStatusAlertTitle = "Title 2";
    String updatedStatusAlertDescription = "Description 2";
    String updatedStatusAlertLink = "This has a link";

    StatusAlert statusAlert =
        new StatusAlert()
            .title(updatedStatusAlertTitle)
            .message(updatedStatusAlertDescription)
            .link(updatedStatusAlertLink)
            .alertLocation(location); // Use the parameterized location

    statusAlertController.postStatusAlert(statusAlert);
    StatusAlert updatedStatusAlert = statusAlertController.getStatusAlert().getBody();
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
