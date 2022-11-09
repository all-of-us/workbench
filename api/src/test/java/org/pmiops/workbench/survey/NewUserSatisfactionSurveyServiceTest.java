package org.pmiops.workbench.survey;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class NewUserSatisfactionSurveyServiceTest {
  @Autowired private NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;
  @MockBean private AccessTierDao mockAccessTierDao;
  @MockBean private UserAccessTierDao mockUserAccessTierDao;
  @MockBean private AccessTierService mockAccessTierService;
  private DbAccessTier registeredAccessTier;

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
    registeredAccessTier = TestMockFactory.createRegisteredTierForTests(mockAccessTierDao);
    when(mockAccessTierService.getRegisteredTierOrThrow()).thenReturn(registeredAccessTier);
    PROVIDED_CLOCK.setInstant(START_INSTANT);
    user = new DbUser();
  }

  // The user has RT access and the enabled date is within the eligibility window
  @Test
  public void testEligibleToTakeSurvey_eligible() {
    final Instant threeWeeksAgo = PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS);
    DbUserAccessTier userAccessTier =
        new DbUserAccessTier().setFirstEnabled(Timestamp.from(threeWeeksAgo));
    when(mockUserAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier))
        .thenReturn(Optional.of(userAccessTier));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isTrue();
  }

  // A user who has taken the survey already is ineligible
  @Test
  public void testEligibleToTakeSurvey_takenSurveyIneligible() {
    final Instant instantWithinEligibilityWindow =
        PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS);
    DbUserAccessTier userAccessTier =
        new DbUserAccessTier().setFirstEnabled(Timestamp.from(instantWithinEligibilityWindow));
    when(mockUserAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier))
        .thenReturn(Optional.of(userAccessTier));

    user.setNewUserSatisfactionSurvey(new DbNewUserSatisfactionSurvey());

    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  // A user without RT access is ineligible
  @Test
  public void testEligibleToTakeSurvey_incompleteRTAccessStepsIneligible() {
    when(mockUserAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier))
        .thenReturn(Optional.empty());
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  // A user with RT access for less than two weeks is ineligible
  @Test
  public void testEligibleToTakeSurvey_usersWithinTwoWeeksIneligible() {
    final Instant twoWeeksMinusOneDayAgo =
        PROVIDED_CLOCK.instant().minus((2 * 7) - 1, ChronoUnit.DAYS);
    DbUserAccessTier userAccessTier =
        new DbUserAccessTier().setFirstEnabled(Timestamp.from(twoWeeksMinusOneDayAgo));
    when(mockUserAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier))
        .thenReturn(Optional.of(userAccessTier));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }

  // A user is ineligible after two months of eligibility
  @Test
  public void testEligibleToTakeSurvey_ineligibleAfterTwoMonths() {
    final Instant twoMonthsTwoWeeksOneDayAgo =
        PROVIDED_CLOCK.instant().minus(61 + (2 * 7) + 1, ChronoUnit.DAYS);
    DbUserAccessTier userAccessTier =
        new DbUserAccessTier().setFirstEnabled(Timestamp.from(twoMonthsTwoWeeksOneDayAgo));
    when(mockUserAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier))
        .thenReturn(Optional.of(userAccessTier));
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isFalse();
  }
}
