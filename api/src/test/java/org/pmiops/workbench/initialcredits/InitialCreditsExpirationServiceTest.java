package org.pmiops.workbench.initialcredits;

import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration.NotificationStatus;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.legacy_leonardo_client.LeonardoApiClient;
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

  @MockBean private MailService mailService;

  @MockBean private FakeClock fakeClock;

  @MockBean private LeonardoApiClient leonardoApiClient;

  @MockBean private InstitutionService institutionService;

  private static final Timestamp NOW = Timestamp.from(FakeClockConfiguration.NOW.toInstant());
  private static final Timestamp NOW_PLUS_ONE_DAY =
      Timestamp.from(FakeClockConfiguration.NOW.toInstant().plusSeconds(24 * 60 * 60));
  private static final Timestamp NOW_MINUS_ONE_DAY =
      Timestamp.from(FakeClockConfiguration.NOW.toInstant().minusSeconds(24 * 60 * 60));

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
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();
    config.billing.accountId = "billingAccountId";
    when(fakeClock.instant()).thenReturn(FakeClockConfiguration.NOW.toInstant());

    workspace =
        spyWorkspaceDao.save(
            new DbWorkspace()
                .setBillingAccountName(config.billing.initialCreditsBillingAccountName())
                .setWorkspaceId(1L));
    when(spyWorkspaceDao.findAllByCreator(any())).thenReturn(Set.of(workspace));
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
  public void test_checkCreditsExpirationForUserIDs_noExpirationRecord() throws MessagingException {
    DbUser user = spyUserDao.save(new DbUser());
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, never()).alertUserInitialCreditsExpired(any());
    verifyUserSaveOnlyDuringSetup();
    // Only run during setup
    verify(spyWorkspaceDao, times(1)).save(any());
    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_bypassed() throws MessagingException {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration().setBypassed(true).setExpirationTime(NOW)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, never()).alertUserInitialCreditsExpired(any());
    verifyUserSaveOnlyDuringSetup();
    // Only run during setup
    verify(spyWorkspaceDao, times(1)).save(any());
    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_unexpired() throws MessagingException {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW_PLUS_ONE_DAY)
                        .setNotificationStatus(NotificationStatus.NO_NOTIFICATION_SENT)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, never()).alertUserInitialCreditsExpired(any());
    verifyUserSaveOnlyDuringSetup();
    // Only run during setup
    verify(spyWorkspaceDao, times(1)).save(any());
    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_expiredPreviouslySentNotification()
      throws MessagingException {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW_MINUS_ONE_DAY)
                        .setNotificationStatus(NotificationStatus.EXPIRATION_NOTIFICATION_SENT)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, never()).alertUserInitialCreditsExpired(any());
    verifyUserSaveOnlyDuringSetup();
    // Only run during setup
    verify(spyWorkspaceDao, times(1)).save(any());
    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_expiredUnsentNotification()
      throws MessagingException {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW_MINUS_ONE_DAY)
                        .setNotificationStatus(NotificationStatus.NO_NOTIFICATION_SENT)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, times(1)).alertUserInitialCreditsExpired(any());
    // Called once during setup and once during the test
    verify(spyUserDao, times(2)).save(any());
    // Once during test setup and once during test
    verify(spyWorkspaceDao, times(2)).save(any());
    verify(leonardoApiClient, times(1)).deleteAllResources(workspace.getGoogleProject(), false);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_expiredUnsentNotificationWithMailException()
      throws MessagingException {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW_MINUS_ONE_DAY)
                        .setNotificationStatus(NotificationStatus.NO_NOTIFICATION_SENT)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));
    doThrow(new MessagingException()).when(mailService).alertUserInitialCreditsExpired(any());

    service.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(mailService, times(1)).alertUserInitialCreditsExpired(any());
    verifyUserSaveOnlyDuringSetup();
    // Once during test setup and once during test
    verify(spyWorkspaceDao, times(2)).save(any());
    verify(leonardoApiClient, times(1)).deleteAllResources(workspace.getGoogleProject(), false);
  }
}
