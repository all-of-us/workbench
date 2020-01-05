package org.pmiops.workbench.statusalerts;

import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class StatusAlertControllerTest {
  private static final Instant NOW = Instant.now();
  private String STATUS_ALERT_INITIAL_TITLE = "Hello World";
  private String STATUS_ALERT_INITIAL_DESCRIPTION = "Status alert description";
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired private StatusAlertController statusAlertController;

  @TestConfiguration
  @Import({StatusAlertController.class})
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() throws Exception {
    StatusAlert initialStatusAlert =
        new StatusAlert()
            .title(STATUS_ALERT_INITIAL_TITLE)
            .message(STATUS_ALERT_INITIAL_DESCRIPTION);
    statusAlertController.postStatusAlert(initialStatusAlert);
  }

  @Test
  public void testGetStatusAlert() {
    StatusAlert statusAlert = statusAlertController.getStatusAlert().getBody();
    assertThat(statusAlert.getTitle()).matches(STATUS_ALERT_INITIAL_TITLE);
    assertThat(statusAlert.getMessage()).matches(STATUS_ALERT_INITIAL_DESCRIPTION);
  }

  @Test
  public void testPostStatusAlert() {
    String updatedStatusAlertTitle = "Title 2";
    String updatedStatusAlertDescription = "Description 2";
    String updatedStatusAlertLink = "This has a link";
    StatusAlert statusAlert =
        new StatusAlert()
            .title(updatedStatusAlertTitle)
            .message(updatedStatusAlertDescription)
            .link(updatedStatusAlertLink);
    statusAlertController.postStatusAlert(statusAlert);
    StatusAlert updatedStatusAlert = statusAlertController.getStatusAlert().getBody();
    assertThat(updatedStatusAlert.getTitle()).matches(updatedStatusAlertTitle);
    assertThat(updatedStatusAlert.getMessage()).matches(updatedStatusAlertDescription);
    assertThat(updatedStatusAlert.getLink()).matches(updatedStatusAlertLink);
  }
}
