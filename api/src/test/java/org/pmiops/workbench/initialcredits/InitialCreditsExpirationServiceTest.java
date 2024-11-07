package org.pmiops.workbench.initialcredits;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
@Import({
  InitialCreditsExpirationServiceImpl.class,
})
public class InitialCreditsExpirationServiceTest {

  @Autowired private InitialCreditsExpirationService service;

  @SpyBean private UserDao spyUserDao;

  @SpyBean private WorkspaceDao spyWorkspaceDao;

  @MockBean private FakeClock fakeClock;

  @MockBean private LeonardoApiClient leonardoApiClient;

  @MockBean private InstitutionService institutionService;

  @MockBean private MailService mailService;

  private static final long validityPeriodDays = 17L; // arbitrary
  private static final long extensionPeriodDays = 78L; // arbitrary
  private static final long warningPeriodDays = 10L; // arbitrary

  private static final Timestamp NOW = Timestamp.from(FakeClockConfiguration.NOW.toInstant());
  private static final Timestamp PAST_EXPIRATION =
      Timestamp.from(FakeClockConfiguration.NOW.toInstant().minusSeconds(24 * 60 * 60));
  private static final Timestamp DURING_WARNING_PERIOD =
      Timestamp.from(
          FakeClockConfiguration.NOW
              .toInstant()
              .plusSeconds((warningPeriodDays - 1L) * 24 * 60 * 60));
  private static final Timestamp BEFORE_WARNING_PERIOD =
      Timestamp.from(
          FakeClockConfiguration.NOW
              .toInstant()
              .plusSeconds((warningPeriodDays + 1L) * 24 * 60 * 60));

  private static WorkbenchConfig config;

  private DbWorkspace workspace;

  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig config() {
      return config;
    }
  }

  @BeforeEach
  public void setUp() throws MessagingException {
    config = WorkbenchConfig.createEmptyConfig();
    config.billing.initialCreditsValidityPeriodDays = validityPeriodDays;
    config.billing.initialCreditsExtensionPeriodDays = extensionPeriodDays;
    config.billing.initialCreditsExpirationWarningDays = warningPeriodDays;
    when(fakeClock.instant()).thenReturn(FakeClockConfiguration.NOW.toInstant());

    workspace =
        spyWorkspaceDao.save(
            new DbWorkspace()
                .setBillingAccountName(config.billing.initialCreditsBillingAccountName())
                .setWorkspaceId(1L));
    when(spyWorkspaceDao.findAllByCreator(any())).thenReturn(Set.of(workspace));
    doNothing().when(mailService).alertUserInitialCreditsExpiring(isA(DbUser.class));
  }

  private void verifyUserSaveOnlyDuringSetup() {
    // Called once during setup but not during the test
    verify(spyUserDao, times(1)).save(any());
  }

  @Test
  public void test_none() {
    DbUser user = new DbUser();
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_userBypassed() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(true).setExpirationTime(NOW));
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_institutionBypassed() {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW)));
    when(institutionService.shouldBypassForCreditsExpiration(user)).thenReturn(true);
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_nullTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExpirationTime(null));
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_validTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExpirationTime(NOW));
    assertThat(service.getCreditsExpiration(user)).hasValue(NOW);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_null() {
    service.checkCreditsExpirationForUserIDs(null);
    verify(spyUserDao, never()).findAllById(any());
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_emptyList() {
    service.checkCreditsExpirationForUserIDs(new ArrayList<>());
    verify(spyUserDao, never()).findAllById(any());
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_noExpirationRecord() {
    DbUser user = spyUserDao.save(new DbUser());
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verifyUserSaveOnlyDuringSetup();
    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
    assertNull(user.getUserInitialCreditsExpiration());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dataForUsersWithAnExpirationRecord")
  public void test_checkCreditsExpirationForUserIDs_withExpirationRecord(
      String testName,
      Timestamp expirationTime,
      boolean bypassed,
      Timestamp expectedWarningNotificationTime,
      Timestamp expectedCleanupTime) {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setExpirationTime(expirationTime)
                        .setBypassed(bypassed)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    assertEquals(
        user.getUserInitialCreditsExpiration().getApproachingExpirationNotificationTime(),
        expectedWarningNotificationTime);
    assertEquals(
        user.getUserInitialCreditsExpiration().getExpirationCleanupTime(), expectedCleanupTime);
  }

  static Stream<Arguments> dataForUsersWithAnExpirationRecord() {
    return Stream.of(
        Arguments.of(
            "If a bypassed user is before their warning period, they will not receive a warning email or have their resources cleaned up.",
            BEFORE_WARNING_PERIOD,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user is before their warning period, they will not receive a warning email or have their resources cleaned up.",
            BEFORE_WARNING_PERIOD,
            false,
            null,
            null),
        Arguments.of(
            "If a bypassed user is in their warning period, they will not receive a warning email or have their resources cleaned up.",
            DURING_WARNING_PERIOD,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user is in their warning period, they will receive a warning email but not have their resources cleaned up.",
            DURING_WARNING_PERIOD,
            false,
            NOW,
            null),
        Arguments.of(
            "If a bypassed user has passed their expiration date, they will not receive a warning email or have their resources cleaned up.",
            PAST_EXPIRATION,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user has passed their expiration date, they will not receive a warning email but will have their resources cleaned up.",
            PAST_EXPIRATION,
            false,
            null,
            NOW));
  }
}
