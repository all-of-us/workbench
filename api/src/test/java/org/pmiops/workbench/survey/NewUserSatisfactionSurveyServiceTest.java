package org.pmiops.workbench.survey;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.OneTimeCodeDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbOneTimeCode;
import org.pmiops.workbench.db.model.DbOneTimeCode.Purpose;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.NewUserSatisfactionSurveySatisfaction;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class NewUserSatisfactionSurveyServiceTest {
  @Autowired private NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;
  @MockBean private NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper;
  @MockBean private MailService mailService;
  @MockBean private UserDao userDao;
  @MockBean private OneTimeCodeDao oneTimeCodeDao;

  @Captor private ArgumentCaptor<DbOneTimeCode> oneTimeCodeCaptor;

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private static final Timestamp ELIGIBLE_CREATION_TIME =
      Timestamp.from(PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS));
  private DbUser user;

  private DbOneTimeCode validOneTimeCode() {
    return new DbOneTimeCode().setUser(user).setPurpose(Purpose.NEW_USER_SATISFACTION_SURVEY);
  }

  private CreateNewUserSatisfactionSurvey createValidFormData() {
    return new CreateNewUserSatisfactionSurvey()
        .satisfaction(NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED)
        .additionalInfo("Love it!");
  }

  @TestConfiguration
  @MockBean({
    NewUserSatisfactionSurveyDao.class,
  })
  @Import({
    NewUserSatisfactionSurveyServiceImpl.class,
  })
  public static class Config {
    @Bean
    Clock clock() {
      return PROVIDED_CLOCK;
    }
  }

  @BeforeEach
  public void setup() {
    PROVIDED_CLOCK.setInstant(START_INSTANT);
    user = new DbUser().setCreationTime(Timestamp.from(START_INSTANT));
  }

  // The user's creation date is within the eligibility window
  @Test
  public void testEligibleToTakeSurvey_eligible() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
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

  @Test
  public void testOneTimeCodeStringValid() {
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isTrue();
  }

  @Test
  public void testOneTimeCodeStringValid_noCodeExistsWithId() {
    final String oneTimeCode = "abc";
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.empty());
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_codeIsUsed() {
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    dbOneTimeCode.setUsedTime(Timestamp.from(Instant.now()));

    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_codeIsNotForNewUserSatisfactionSurvey() {
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    dbOneTimeCode.setPurpose(null);

    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_userIsIneligibleForSurvey() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";

    // user has already taken the survey
    user.setNewUserSatisfactionSurvey(new DbNewUserSatisfactionSurvey());

    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testCreateNewUserSatisfactionSurveyWithOneTimeCode()
      throws InvalidOneTimeCodeException {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));

    newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
        formData, oneTimeCode);

    verify(newUserSatisfactionSurveyMapper).toDbNewUserSatisfactionSurvey(formData, user);
    verify(oneTimeCodeDao).save(dbOneTimeCode);
    assertThat(dbOneTimeCode.getUsedTime()).isEqualTo(Timestamp.from(START_INSTANT));
  }

  @Test
  public void
      testCreateNewUserSatisfactionSurveyWithOneTimeCode_throwsExceptionWhenCodeDoesNotExist() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final String oneTimeCode = "abc";
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.empty());

    assertThrows(
        InvalidOneTimeCodeException.class,
        () ->
            newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
                formData, oneTimeCode));
  }

  // additional invalid one-time code cases are tested in testOneTimeCodeStringValid_* tests
  @Test
  public void
      testCreateNewUserSatisfactionSurveyWithOneTimeCode_throwsExceptionWhenCodeIsInvalid() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbOneTimeCode dbOneTimeCode = validOneTimeCode();
    final String oneTimeCode = "abc";
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(oneTimeCodeDao.findByStringId(oneTimeCode)).thenReturn(Optional.of(dbOneTimeCode));

    dbOneTimeCode.setUsedTime(Timestamp.from(START_INSTANT));

    assertThrows(
        InvalidOneTimeCodeException.class,
        () ->
            newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
                formData, oneTimeCode));
  }
}
