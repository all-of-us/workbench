package org.pmiops.workbench.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;
import static org.pmiops.workbench.utils.PresetData.createDbUser;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserInitialCreditsExpirationDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;

@ExtendWith(MockitoExtension.class)
public class AccessSyncServiceTest {

  @Mock private AccessTierService accessTierService;

  @Mock private UserDao userDao;

  @Mock UserInitialCreditsExpirationDao userInitialCreditsExpirationDao;

  @Mock Provider<WorkbenchConfig> workbenchConfigProvider;

  @Mock AccessModuleService accessModuleService;

  @Mock InstitutionService institutionService;

  @Mock UserServiceAuditor userServiceAuditor;

  Instant now = Instant.parse("2000-01-01T00:00:00.00Z");

  @Spy Clock clock = Clock.fixed(now, ZoneId.systemDefault());

  @InjectMocks private AccessSyncServiceImpl accessSyncService;

  private DbAccessTier registeredTier;
  private DbUser dbUser;
  private Agent agent;

  @BeforeEach
  public void setUp() {
    Mockito.reset(
        accessTierService,
        userDao,
        userInitialCreditsExpirationDao,
        workbenchConfigProvider,
        accessModuleService,
        institutionService,
        userServiceAuditor);
    registeredTier = createRegisteredTier();
    when(accessTierService.getAllTiers()).thenReturn(List.of(registeredTier));
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    dbUser = createDbUser();
    dbUser.setDisabled(false);
    Institution institution = new Institution();
    agent = Agent.asUser(dbUser);

    // Ensures that users meets all requirements for any tier
    when(accessModuleService.isModuleCompliant(any(), any())).thenReturn(true);
    when(institutionService.getByUser(any())).thenReturn(Optional.of(institution));
    when(institutionService.validateInstitutionalEmail(any(), any(), any())).thenReturn(true);
    Mockito.lenient()
        .when(userInitialCreditsExpirationDao.findByUser(dbUser))
        .thenReturn(Optional.empty());

    when(userDao.save(any())).thenAnswer(i -> i.getArguments()[0]);
  }

  private void stubWorkbenchConfig_enableInitialCreditsExpiration(boolean enable) {

    WorkbenchConfig workbenchConfig = createEmptyConfig();
    workbenchConfig.featureFlags.enableInitialCreditsExpiration = enable;
    workbenchConfig.access.enableRasLoginGovLinking = false;
    workbenchConfig.billing.freeTierCreditValidityPeriodDays = 57L;

    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
  }

  private void assertCreditExpirationEquality(
      DbUserInitialCreditsExpiration expected, DbUserInitialCreditsExpiration actual) {
    assertEquals(expected.getUser(), actual.getUser());
    assertEquals(expected.getCreditStartTime(), actual.getCreditStartTime());
    assertEquals(expected.getExpirationTime(), actual.getExpirationTime());
    assertEquals(expected.getExtensionCount(), actual.getExtensionCount());
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  public void testUpdateUserAccessTiers_whenRTGranted(boolean enableInitialCreditsExpiration) {
    stubWorkbenchConfig_enableInitialCreditsExpiration(enableInitialCreditsExpiration);

    // User starts with access to no tiers
    List<DbAccessTier> oldAccessTiers = List.of();
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    DbUserInitialCreditsExpiration expected = new DbUserInitialCreditsExpiration();

    expected.setUser(dbUser);

    if (enableInitialCreditsExpiration) {
      ArgumentCaptor<DbUserInitialCreditsExpiration> captor =
          ArgumentCaptor.forClass(DbUserInitialCreditsExpiration.class);
      verify(userInitialCreditsExpirationDao).save(captor.capture());
      DbUserInitialCreditsExpiration actual = captor.getValue();
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      Timestamp expirationTime =
          new Timestamp(
              now.getTime()
                  + TimeUnit.DAYS.toMillis(
                      workbenchConfigProvider.get().billing.freeTierCreditValidityPeriodDays));
      expected.setCreditStartTime(now);
      expected.setExpirationTime(expirationTime);
      assertCreditExpirationEquality(expected, actual);
    } else {
      verify(userInitialCreditsExpirationDao, Mockito.never()).save(any());
    }

    assertEquals(updatedUser, dbUser);
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  public void testUpdateUserAccessTiers_whenAccessTiersAreUnchanged(
      boolean enableInitialCreditsExpiration) {
    stubWorkbenchConfig_enableInitialCreditsExpiration(enableInitialCreditsExpiration);

    // User starts with access to no tiers
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    verify(userInitialCreditsExpirationDao, Mockito.never()).save(any());
    assertEquals(updatedUser, dbUser);
  }

  @Test
  public void
      testUpdateUserAccessTiers_existingCreditExpirationDoesNotChangeWithAdditionalAccess() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(true);

    DbAccessTier controlledTier = createControlledTier();
    when(accessTierService.getAllTiers()).thenReturn(List.of(registeredTier, controlledTier));
    when(accessTierService.getAccessTierByName(controlledTier.getShortName()))
        .thenReturn(Optional.of(controlledTier));

    // User starts with access to registered tier
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUserInitialCreditsExpiration existingExpiration =
        new DbUserInitialCreditsExpiration()
            .setUser(dbUser)
            .setCreditStartTime(new Timestamp(now.toEpochMilli()))
            .setExpirationTime(new Timestamp(now.toEpochMilli() + TimeUnit.DAYS.toMillis(57L)))
            .setBypassed(false)
            .setExtensionCount(0);
    when(userInitialCreditsExpirationDao.findByUser(dbUser))
        .thenReturn(Optional.of(existingExpiration));

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    verify(userInitialCreditsExpirationDao, Mockito.never()).save(any());
    assertEquals(updatedUser, dbUser);
    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }

  @Test
  public void testUpdateUserAccessTiers_noExpirationForSubsequentAccessIfNoneToBeginWith() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(true);

    DbAccessTier controlledTier = createControlledTier();
    when(accessTierService.getAllTiers()).thenReturn(List.of(registeredTier, controlledTier));
    when(accessTierService.getAccessTierByName(controlledTier.getShortName()))
        .thenReturn(Optional.of(controlledTier));

    // User starts with access to registered tier
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    verify(userInitialCreditsExpirationDao, Mockito.never()).save(any());
    assertEquals(updatedUser, dbUser);

    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }
}
