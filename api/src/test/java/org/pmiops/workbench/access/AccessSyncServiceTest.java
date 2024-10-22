package org.pmiops.workbench.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;
import static org.pmiops.workbench.utils.PresetData.createDbUser;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;

@ExtendWith(MockitoExtension.class)
public class AccessSyncServiceTest {

  @Mock private AccessTierService accessTierService;

  @Mock private UserDao userDao;

  @Mock Provider<WorkbenchConfig> workbenchConfigProvider;

  @Mock AccessModuleService accessModuleService;

  @Mock InstitutionService institutionService;

  @Mock UserService userService;

  @Mock UserServiceAuditor userServiceAuditor;

  Instant now = Instant.parse("2000-01-01T00:00:00.00Z");


  @InjectMocks private AccessSyncServiceImpl accessSyncService;

  private DbAccessTier registeredTier;
  private DbUser dbUser;
  private Agent agent;

  @BeforeEach
  public void setUp() {
    Mockito.reset(
        accessTierService,
        userDao,
        workbenchConfigProvider,
        accessModuleService,
        institutionService,
        userService,
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

    when(userDao.save(any())).thenAnswer(i -> i.getArguments()[0]);
  }

  private void stubWorkbenchConfig_enableInitialCreditsExpiration(boolean enable) {

    WorkbenchConfig workbenchConfig = createEmptyConfig();
    workbenchConfig.featureFlags.enableInitialCreditsExpiration = enable;
    workbenchConfig.access.enableRasLoginGovLinking = false;
    workbenchConfig.billing.initialCreditsValidityPeriodDays = 57L;

    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  public void testUpdateUserAccessTiers_whenRTGranted(boolean enableInitialCreditsExpiration) {
    stubWorkbenchConfig_enableInitialCreditsExpiration(enableInitialCreditsExpiration);

    // User starts with access to no tiers
    List<DbAccessTier> oldAccessTiers = List.of();
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    verify(userService, times(enableInitialCreditsExpiration ? 1 : 0))
        .createInitialCreditsExpiration(dbUser);
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
    dbUser.setUserInitialCreditsExpiration(existingExpiration);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

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

    assertEquals(updatedUser, dbUser);

    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }
}
