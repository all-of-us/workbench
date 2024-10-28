package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.FakeClockConfiguration.CLOCK;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration.NotificationStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationServiceImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InitialCreditsExpirationServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = FakeClockConfiguration.NOW.toInstant();
  private static DbUser providedDbUser;
  private static WorkbenchConfig providedWorkbenchConfig;
  private static List<DbAccessModule> accessModules;

  @MockBean private InstitutionService mockInstitutionService;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private UserDao userDao;
  @Autowired private InitialCreditsExpirationService initialCreditsExpirationService;

  @Import({
    InitialCreditsExpirationServiceImpl.class,
  })
  @MockBean({
    MailService.class,
    UserService.class,
    AccessTierServiceImpl.class,
    UserServiceAuditor.class,
    LeonardoApiClient.class,
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    Random getRandom() {
      return new Random();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return providedDbUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }

    @Bean
    public Clock getClock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() {
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.termsOfService.minimumAcceptedAouVersion = 5; // arbitrary
    providedWorkbenchConfig.billing.initialCreditsValidityPeriodDays = 17L; // arbitrary
    providedWorkbenchConfig.billing.initialCreditsExtensionPeriodDays = 78L; // arbitrary

    DbUserInitialCreditsExpiration initialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setBypassed(false)
            .setCreditStartTime(Timestamp.from(START_INSTANT))
            .setExpirationTime(
                Timestamp.from(
                    START_INSTANT.plus(
                        providedWorkbenchConfig.billing.initialCreditsValidityPeriodDays,
                        ChronoUnit.DAYS)))
            .setExtensionCount(0)
            .setNotificationStatus(NotificationStatus.NO_NOTIFICATION_SENT);

    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    user.setUserInitialCreditsExpiration(initialCreditsExpiration);
    user = userDao.save(user);
    providedDbUser = user;

    // key UserService logic depends on the existence of the Registered Tier
    accessTierDao.save(createRegisteredTier());

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
    Institution institution = new Institution();
    when(mockInstitutionService.getByUser(user)).thenReturn(Optional.of(institution));
    when(mockInstitutionService.validateInstitutionalEmail(
            institution, user.getContactEmail(), REGISTERED_TIER_SHORT_NAME))
        .thenReturn(true);
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  public void testSetInitialCreditsExpirationBypassed(
      boolean initialBypassed, boolean expectedBypassed) {
    providedDbUser.getUserInitialCreditsExpiration().setBypassed(initialBypassed);
    initialCreditsExpirationService.setInitialCreditsExpirationBypassed(
        providedDbUser, expectedBypassed);
    assertThat(providedDbUser.getUserInitialCreditsExpiration().isBypassed())
        .isEqualTo(expectedBypassed);
  }

  @Test
  public void testCreateInitialCreditsExpiration() {
    DbUser user = new DbUser();
    user.setUsername("Test User");
    user = userDao.save(user);
    assertThat(user.getUserInitialCreditsExpiration()).isNull();

    initialCreditsExpirationService.createInitialCreditsExpiration(user);

    DbUserInitialCreditsExpiration initialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    assertThat(initialCreditsExpiration).isNotNull();
    assertThat(initialCreditsExpiration.getCreditStartTime()).isEqualTo(FakeClockConfiguration.NOW);

    Timestamp expectedExpirationTime =
        Timestamp.from(
            FakeClockConfiguration.NOW
                .toInstant()
                .atZone(CLOCK.getZone())
                .plusDays(providedWorkbenchConfig.billing.initialCreditsValidityPeriodDays)
                .toInstant());
    assertThat(initialCreditsExpiration.getExpirationTime()).isEqualTo(expectedExpirationTime);
    assertThat(initialCreditsExpiration.getExtensionCount()).isEqualTo(0);
    assertThat(initialCreditsExpiration.getNotificationStatus())
        .isEqualTo(NotificationStatus.NO_NOTIFICATION_SENT);
    assertThat(initialCreditsExpiration.isBypassed()).isFalse();
  }

  @Test
  public void testExtendInitialCreditsExpiration() {
    DbUserInitialCreditsExpiration initialCreditsExpiration =
        providedDbUser.getUserInitialCreditsExpiration();
    assertThat(initialCreditsExpiration).isNotNull();

    initialCreditsExpirationService.extendInitialCreditsExpiration(providedDbUser);
    assertThat(initialCreditsExpiration.getExpirationTime())
        .isEqualTo(
            new Timestamp(
                initialCreditsExpiration.getCreditStartTime().getTime()
                    + TimeUnit.DAYS.toMillis(
                        providedWorkbenchConfig.billing.initialCreditsExtensionPeriodDays)));
  }

  @Test
  public void testExtendInitialCreditsExpiration_notInitiated() {

    providedDbUser.setUserInitialCreditsExpiration(null);
    userDao.save(providedDbUser);

    BadRequestException actualException = assertThrows(
        BadRequestException.class,
        () -> initialCreditsExpirationService.extendInitialCreditsExpiration(providedDbUser));
    assertThat(actualException.getMessage()).isEqualTo("User does not have initial credits expiration set.");
  }

  @Test
  public void testExtendInitialCreditsExpiration_alreadyExtended() {

    providedDbUser.getUserInitialCreditsExpiration().setExtensionCount(1);
    userDao.save(providedDbUser);

    BadRequestException actualException = assertThrows(
        BadRequestException.class,
        () -> initialCreditsExpirationService.extendInitialCreditsExpiration(providedDbUser));
    assertThat(actualException.getMessage()).isEqualTo("User has already extended their initial credits expiration and cannot extend further.");
  }
}
