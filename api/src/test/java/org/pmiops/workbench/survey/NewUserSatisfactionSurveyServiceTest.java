package org.pmiops.workbench.survey;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class NewUserSatisfactionSurveyServiceTest {
  @Autowired private NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private DbUser user;

  @TestConfiguration
  @Import({NewUserSatisfactionSurveyServiceImpl.class})
  public static class Config {
    @Bean
    Clock clock() {
      return PROVIDED_CLOCK;
    }
  }

  @BeforeEach
  public void setup() {
    PROVIDED_CLOCK.setInstant(START_INSTANT);
    user = new DbUser();
  }

  // The user's creation date is within the eligibility window
  @Test
  public void testEligibleToTakeSurvey_eligible() {
    final Instant threeWeeksAgo = PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS);
    user.setCreationTime(Timestamp.from(threeWeeksAgo));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isTrue();
  }

  // A user who has taken the survey already is ineligible
  @Test
  public void testEligibleToTakeSurvey_takenSurveyIneligible() {
    final Instant instantWithinEligibilityWindow =
        PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS);
    user.setCreationTime(Timestamp.from(instantWithinEligibilityWindow));

    user.setNewUserSatisfactionSurvey(new DbNewUserSatisfactionSurvey());

    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  // A user created less than two weeks is ineligible
  @Test
  public void testEligibleToTakeSurvey_usersWithinTwoWeeksIneligible() {
    final Instant twoWeeksMinusOneDayAgo =
        PROVIDED_CLOCK.instant().minus((2 * 7) - 1, ChronoUnit.DAYS);
    user.setCreationTime(Timestamp.from(twoWeeksMinusOneDayAgo));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  // A user is ineligible after two months of eligibility
  @Test
  public void testEligibleToTakeSurvey_ineligibleAfterTwoMonths() {
    final Instant twoMonthsTwoWeeksOneDayAgo =
        PROVIDED_CLOCK.instant().minus(61 + (2 * 7) + 1, ChronoUnit.DAYS);
    user.setCreationTime(Timestamp.from(twoMonthsTwoWeeksOneDayAgo));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  @Test
  public void testEligibilityWindowEnd() {
    user.setCreationTime(Timestamp.from(PROVIDED_CLOCK.instant()));
    final Instant twoMonthsTwoWeeksFromNow =
        PROVIDED_CLOCK.instant().plus(61 + (2 * 7), ChronoUnit.DAYS);
    assertThat(newUserSatisfactionSurveyService.eligibilityWindowEnd(user))
        .isEqualTo(twoMonthsTwoWeeksFromNow);
  }
}
