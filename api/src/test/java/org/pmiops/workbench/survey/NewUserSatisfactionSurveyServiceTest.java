package org.pmiops.workbench.survey;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyOneTimeCodeDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyOneTimeCode;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
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
  @MockBean private NewUserSatisfactionSurveyOneTimeCodeDao newUserSatisfactionSurveyOneTimeCodeDao;

  @Captor private ArgumentCaptor<DbNewUserSatisfactionSurveyOneTimeCode> oneTimeCodeCaptor;

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private static final Timestamp ELIGIBLE_CREATION_TIME =
      Timestamp.from(PROVIDED_CLOCK.instant().minus(3 * 7, ChronoUnit.DAYS));
  private static final String UI_BASE_URL = "example.com";
  private DbUser user;

  final String VALID_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

  private DbNewUserSatisfactionSurveyOneTimeCode validOneTimeCode() {
    return new DbNewUserSatisfactionSurveyOneTimeCode().setUser(user);
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

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.server.uiBaseUrl = UI_BASE_URL;
      return workbenchConfig;
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
    assertThat(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).isTrue();
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
    final DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
        validOneTimeCode();
    final String oneTimeCode = VALID_UUID;
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.of(dbNewUserSatisfactionSurveyOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isTrue();
  }

  @Test
  public void testOneTimeCodeStringValid_noCodeExistsWithId() {
    final String oneTimeCode = VALID_UUID;
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.empty());
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_codeIsNotAUuid() {
    final String oneTimeCode = "not_a_uuid";
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_codeIsUsed() {
    final DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
        validOneTimeCode();
    final String oneTimeCode = VALID_UUID;
    user.setCreationTime(ELIGIBLE_CREATION_TIME);

    dbNewUserSatisfactionSurveyOneTimeCode.setUsedTime(Timestamp.from(Instant.now()));

    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.of(dbNewUserSatisfactionSurveyOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testOneTimeCodeStringValid_userIsIneligibleForSurvey() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
        validOneTimeCode();
    final String oneTimeCode = VALID_UUID;

    // user has already taken the survey
    user.setNewUserSatisfactionSurvey(new DbNewUserSatisfactionSurvey());

    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.of(dbNewUserSatisfactionSurveyOneTimeCode));
    assertThat(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).isFalse();
  }

  @Test
  public void testCreateNewUserSatisfactionSurveyWithOneTimeCode() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
        validOneTimeCode();
    final String oneTimeCode = VALID_UUID;
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.of(dbNewUserSatisfactionSurveyOneTimeCode));

    newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
        formData, oneTimeCode);

    verify(newUserSatisfactionSurveyMapper).toDbNewUserSatisfactionSurvey(formData, user);
    verify(newUserSatisfactionSurveyOneTimeCodeDao).save(dbNewUserSatisfactionSurveyOneTimeCode);
    assertThat(dbNewUserSatisfactionSurveyOneTimeCode.getUsedTime())
        .isEqualTo(Timestamp.from(START_INSTANT));
  }

  @Test
  public void
      testCreateNewUserSatisfactionSurveyWithOneTimeCode_throwsExceptionWhenCodeDoesNotExist() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final String oneTimeCode = VALID_UUID;
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.empty());

    assertThrows(
        ForbiddenException.class,
        () ->
            newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
                formData, oneTimeCode));
  }

  @Test
  public void
      testCreateNewUserSatisfactionSurveyWithOneTimeCode_throwsExceptionWhenCodeIsNotAUuid() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final String oneTimeCode = "not_a_uuid";
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();

    assertThrows(
        ForbiddenException.class,
        () ->
            newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
                formData, oneTimeCode));
  }

  // additional invalid one-time code cases are tested in testOneTimeCodeStringValid_* tests
  @Test
  public void
      testCreateNewUserSatisfactionSurveyWithOneTimeCode_throwsExceptionWhenCodeIsInvalid() {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    final DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
        validOneTimeCode();
    final String oneTimeCode = VALID_UUID;
    final CreateNewUserSatisfactionSurvey formData = createValidFormData();
    when(newUserSatisfactionSurveyOneTimeCodeDao.findById(UUID.fromString(oneTimeCode)))
        .thenReturn(Optional.of(dbNewUserSatisfactionSurveyOneTimeCode));

    dbNewUserSatisfactionSurveyOneTimeCode.setUsedTime(Timestamp.from(START_INSTANT));

    assertThrows(
        ForbiddenException.class,
        () ->
            newUserSatisfactionSurveyService.createNewUserSatisfactionSurveyWithOneTimeCode(
                formData, oneTimeCode));
  }

  @Test
  public void testEmailNewUserSatisfactionSurveyLinks() throws MessagingException {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    when(userDao.findUsersBetweenCreationTimeWithoutNewUserSurveyOrCode(
            Timestamp.from(
                START_INSTANT.minus(
                    NewUserSatisfactionSurveyServiceImpl.TWO_WEEKS_DAYS
                        + NewUserSatisfactionSurveyServiceImpl.TWO_MONTHS_DAYS,
                    ChronoUnit.DAYS)),
            Timestamp.from(
                START_INSTANT.minus(
                    NewUserSatisfactionSurveyServiceImpl.TWO_WEEKS_DAYS, ChronoUnit.DAYS))))
        .thenReturn(ImmutableList.of(user));
    String oneTimeCodeString = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    DbNewUserSatisfactionSurveyOneTimeCode oneTimeCode = validOneTimeCode();
    oneTimeCode.setId(UUID.fromString(oneTimeCodeString));
    when(newUserSatisfactionSurveyOneTimeCodeDao.save(any())).thenReturn(oneTimeCode);

    newUserSatisfactionSurveyService.emailNewUserSatisfactionSurveyLinks();

    verify(newUserSatisfactionSurveyOneTimeCodeDao).save(oneTimeCodeCaptor.capture());
    assertThat(oneTimeCodeCaptor.getValue().getUsedTime()).isNull();
    assertThat(oneTimeCodeCaptor.getValue().getUser()).isEqualTo(user);
    verify(mailService)
        .sendNewUserSatisfactionSurveyEmail(user, UI_BASE_URL + "?surveyCode=" + oneTimeCodeString);
  }

  @Test
  public void testEmailNewUserSatisfactionSurveyLinks_throwsExceptionWhenMailerFails()
      throws MessagingException {
    user.setCreationTime(ELIGIBLE_CREATION_TIME);
    when(userDao.findUsersBetweenCreationTimeWithoutNewUserSurveyOrCode(any(), any()))
        .thenReturn(ImmutableList.of(user));
    DbNewUserSatisfactionSurveyOneTimeCode oneTimeCode =
        validOneTimeCode().setId(UUID.randomUUID());
    when(newUserSatisfactionSurveyOneTimeCodeDao.save(any())).thenReturn(oneTimeCode);

    doThrow(new MessagingException())
        .when(mailService)
        .sendNewUserSatisfactionSurveyEmail(any(), any());

    assertThrows(
        ServerErrorException.class,
        () -> newUserSatisfactionSurveyService.emailNewUserSatisfactionSurveyLinks());
  }
}
