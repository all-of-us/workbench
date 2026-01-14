package org.pmiops.workbench.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForRegisteredTierAccess;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;
import static org.pmiops.workbench.utils.PresetData.createDbUser;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
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

  @Mock InitialCreditsService initialCreditsService;

  @Mock UserServiceAuditor userServiceAuditor;

  @Mock TaskQueueService taskQueueService;

  Instant now = Instant.parse("2000-01-01T00:00:00.00Z");

  @InjectMocks private AccessSyncServiceImpl accessSyncService;

  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;
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
        userServiceAuditor,
        taskQueueService);

    registeredTier = createRegisteredTier();
    controlledTier = createControlledTier();

    // Mock dependencies so that updateUserAccessTiers can function
    when(accessTierService.getRegisteredTierOrThrow()).thenReturn(registeredTier);
    when(accessTierService.getAccessTierByName("controlled"))
        .thenReturn(Optional.of(controlledTier));

    dbUser = createDbUser();
    dbUser.setDisabled(false);
    dbUser.setContactEmail("test@example.com"); // Set contact email for tier validation
    Institution institution = new Institution();
    agent = Agent.asUser(dbUser);

    // Mock institution service to return valid institution
    when(institutionService.getByUser(any())).thenReturn(Optional.of(institution));
    when(institutionService.validateInstitutionalEmail(any(), any(), any())).thenReturn(true);

    // Setup default compliance for all modules
    when(accessModuleService.isModuleCompliant(any(), any())).thenReturn(true);

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
  public void testUpdateUserAccessTiers_whenTierGranted(boolean enableInitialCreditsExpiration) {
    stubWorkbenchConfig_enableInitialCreditsExpiration(enableInitialCreditsExpiration);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    // User starts with access to no tiers.
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(List.of());

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    verify(initialCreditsService, times(enableInitialCreditsExpiration ? 1 : 0))
        .createInitialCreditsExpiration(dbUser);
    verify(accessTierService).addUserToTier(dbUser, registeredTier);
    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  public void testUpdateUserAccessTiers_whenAccessTiersAreUnchanged(
      boolean enableInitialCreditsExpiration) {
    stubWorkbenchConfig_enableInitialCreditsExpiration(enableInitialCreditsExpiration);

    // User starts and ends with access to the same tiers.
    // The `isModuleCompliant` setup in `setUp()` ensures the "new" list will also contain these
    // tiers.
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier, controlledTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    assertEquals(updatedUser, dbUser);
    verify(accessTierService, times(0)).addUserToTier(any(), any());
    verify(accessTierService, times(0)).removeUserFromTier(any(), any());
  }

  @Test
  public void testUpdateUserAccessTiers_whenTierIsRemoved() {
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    List<DbAccessTier> oldAccessTiers = List.of(registeredTier, controlledTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    // User remains compliant for RT but loses CT compliance
    // First set RT modules to compliant
    getRequiredModulesForRegisteredTierAccess()
        .forEach(
            moduleName ->
                when(accessModuleService.isModuleCompliant(dbUser, moduleName)).thenReturn(true));
    // Then set CT-specific module to non-compliant
    when(accessModuleService.isModuleCompliant(dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING))
        .thenReturn(false);

    stubWorkbenchConfig_enableInitialCreditsExpiration(true);

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    // Verify that the user was removed from the controlled tier.
    verify(accessTierService).removeUserFromTier(dbUser, controlledTier);
    verify(accessTierService, times(0)).addUserToTier(any(), any());
  }

  @Test
  public void testUpdateUserAccessTiers_whenTiersAreAddedAndRemoved() {
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    // A user starts with only controlled tier access.
    List<DbAccessTier> oldAccessTiers = List.of(controlledTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    // User remains compliant for RT but loses CT compliance
    // First set RT modules to compliant
    getRequiredModulesForRegisteredTierAccess()
        .forEach(
            moduleName ->
                when(accessModuleService.isModuleCompliant(dbUser, moduleName)).thenReturn(true));
    // Then set CT-specific module to non-compliant
    when(accessModuleService.isModuleCompliant(dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING))
        .thenReturn(false);

    stubWorkbenchConfig_enableInitialCreditsExpiration(true);

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    // Verify that the user was added to the registered tier and removed from the controlled tier.
    verify(accessTierService).addUserToTier(dbUser, registeredTier);
    verify(accessTierService).removeUserFromTier(dbUser, controlledTier);
  }

  @Test
  public void
      testUpdateUserAccessTiers_existingCreditExpirationDoesNotChangeWithAdditionalAccess() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(true);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    // User starts with access to registered tier
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUserInitialCreditsExpiration existingExpiration =
        new DbUserInitialCreditsExpiration()
            .setUser(dbUser)
            .setCreditStartTime(new Timestamp(now.toEpochMilli()))
            .setExpirationTime(new Timestamp(now.toEpochMilli() + TimeUnit.DAYS.toMillis(57L)))
            .setBypassed(false)
            .setExtensionTime(null);
    dbUser.setUserInitialCreditsExpiration(existingExpiration);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    assertEquals(updatedUser, dbUser);
    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }

  @Test
  public void testUpdateUserAccessTiers_noExpirationForSubsequentAccessIfNoneToBeginWith() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(true);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    // User starts with access to registered tier
    List<DbAccessTier> oldAccessTiers = List.of(registeredTier);
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(oldAccessTiers);

    DbUser updatedUser = accessSyncService.updateUserAccessTiers(dbUser, agent);

    assertEquals(updatedUser, dbUser);
    verify(accessTierService).addUserToTier(dbUser, controlledTier);
  }

  @Test
  public void testUpdateUserAccessTiers_userWithNoTierGainsTierAccess() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(false);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());
    // Mock VWB services for first tier access
    doNothing().when(taskQueueService).pushVwbPodCreationTask(any());

    // User starts with access to no tiers.
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(List.of());

    // User is compliant for RT modules but not CT (CT module is false)
    when(accessModuleService.isModuleCompliant(dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING))
        .thenReturn(false);

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    // Verify that the user was added to the registered tier only
    verify(accessTierService).addUserToTier(dbUser, registeredTier);
    verify(accessTierService, times(0)).addUserToTier(dbUser, controlledTier);
    verify(accessTierService, times(0)).removeUserFromTier(any(), any());
  }

  @Test
  public void testUpdateUserAccessTiers_userWithNoTierGainsBothTiers() {
    stubWorkbenchConfig_enableInitialCreditsExpiration(false);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());
    // Mock VWB services for first tier access
    doNothing().when(taskQueueService).pushVwbPodCreationTask(any());

    // User starts with access to no tiers.
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(List.of());

    // User is compliant for all modules (RT + CT) - using default setup from setUp()

    accessSyncService.updateUserAccessTiers(dbUser, agent);

    // Verify that the user was added to both tiers
    verify(accessTierService).addUserToTier(dbUser, registeredTier);
    verify(accessTierService).addUserToTier(dbUser, controlledTier);
    verify(accessTierService, times(0)).removeUserFromTier(any(), any());
  }

  @Test
  public void testUpdateUserAccessTiers_concurrentCallsForSameUser_onlyCreatesVwbPodOnce()
      throws InterruptedException {
    stubWorkbenchConfig_enableInitialCreditsExpiration(false);
    doNothing().when(userServiceAuditor).fireUpdateAccessTiersAction(any(), any(), any(), any());

    // Track how many times VWB user/pod creation is called
    AtomicInteger vwbPodTaskCount = new AtomicInteger(0);

    Mockito.doAnswer(
            invocation -> {
              vwbPodTaskCount.incrementAndGet();
              return null;
            })
        .when(taskQueueService)
        .pushVwbPodCreationTask(any());

    // User starts with access to no tiers
    when(accessTierService.getAccessTiersForUser(dbUser)).thenReturn(List.of());

    // Simulate concurrent calls to updateUserAccessTiers for the same user
    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              accessSyncService.updateUserAccessTiers(dbUser, agent);
            } finally {
              latch.countDown();
            }
          });
    }

    // Wait for all threads to complete
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify that VWB pod creation was only called once despite concurrent calls
    assertEquals(
        1,
        vwbPodTaskCount.get(),
        "VWB pod creation task should only be pushed once despite concurrent calls");
  }
}
