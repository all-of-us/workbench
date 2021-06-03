package org.pmiops.workbench.billing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.billing.BillingProjectBufferService.PROJECT_BILLING_ID_SIZE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import org.javers.common.collections.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.CallsRealMethodsWithDelay;
import org.pmiops.workbench.TestLock;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao.ProjectCountByStatusAndTier;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus.CreationStatusEnum;
import org.pmiops.workbench.model.AvailableBufferPerTier;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
public class BillingProjectBufferServiceTest {
  private static final Logger log =
      Logger.getLogger(BillingProjectBufferServiceTest.class.getName());

  private static final Instant NOW = Instant.now();
  private static final Instant BEFORE_CREATING_TIMEOUT_ELAPSES =
      NOW.plus(BillingProjectBufferService.CREATING_TIMEOUT.minusMinutes(1));
  private static final Instant AFTER_CREATING_TIMEOUT_ELAPSED =
      NOW.plus(BillingProjectBufferService.CREATING_TIMEOUT.plusMinutes(1));
  private static final Instant BEFORE_ASSIGNING_TIMEOUT_ELAPSES =
      NOW.plus(BillingProjectBufferService.ASSIGNING_TIMEOUT.minusMinutes(1));
  private static final Instant AFTER_ASSIGNING_TIMEOUT_ELAPSED =
      NOW.plus(BillingProjectBufferService.ASSIGNING_TIMEOUT.plusMinutes(1));
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    BillingProjectBufferService.class,
  })
  @MockBean({
    FireCloudService.class,
    MonitoringService.class,
  })
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope("prototype")
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  private static WorkbenchConfig workbenchConfig;

  @Autowired EntityManager entityManager;
  @Autowired private Clock clock;
  @Autowired private UserDao userDao;
  @Autowired private Provider<WorkbenchConfig> workbenchConfigProvider;
  // Put a spy on the buffer entry DAO to allow us to intercept calls for the buffer recovery test.
  @Autowired @SpyBean private BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  @Autowired private FireCloudService mockFireCloudService;
  @Autowired private MonitoringService mockMonitoringService;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessTierService accessTierService;

  @Autowired private BillingProjectBufferService billingProjectBufferService;

  private final int REGISTERED_TIER_BUFFER_CAPACITY = 5;

  private DbAccessTier registeredTier;

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.projectNamePrefix = "test-prefix";
    workbenchConfig.billing.bufferRefillProjectsPerTask = 1;
    workbenchConfig.billing.bufferStatusChecksPerTask = 10;

    CLOCK.setInstant(NOW);

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    workbenchConfig.billing.bufferCapacityPerTier =
        Collections.singletonMap(registeredTier.getShortName(), REGISTERED_TIER_BUFFER_CAPACITY);

    billingProjectBufferEntryDao = spy(billingProjectBufferEntryDao);
    TestLock lock = new TestLock();
    doAnswer(invocation -> lock.lock()).when(billingProjectBufferEntryDao).acquireAssigningLock();
    doAnswer(invocation -> lock.release())
        .when(billingProjectBufferEntryDao)
        .releaseAssigningLock();

    mockMonitoringService = spy(mockMonitoringService);

    billingProjectBufferService =
        new BillingProjectBufferService(
            accessTierService,
            billingProjectBufferEntryDao,
            clock,
            mockFireCloudService,
            workbenchConfigProvider);
  }

  @Test
  public void canBufferMultipleProjectsPerTask() {
    workbenchConfig.billing.bufferRefillProjectsPerTask = 2;
    billingProjectBufferService.bufferBillingProjects();

    verify(mockFireCloudService, times(2)).createAllOfUsBillingProject(anyString(), anyString());
  }

  @Test
  public void fillBuffer() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> projectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> perimeterCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService)
        .createAllOfUsBillingProject(projectCaptor.capture(), perimeterCaptor.capture());

    String billingProjectName = projectCaptor.getValue();
    String servicePerimeter = perimeterCaptor.getValue();
    assertThat(billingProjectName).startsWith(workbenchConfig.billing.projectNamePrefix);
    assertThat(servicePerimeter).isEqualTo(registeredTier.getServicePerimeter());
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);
  }

  @Test
  public void fillBufferMultiTier() {
    DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    workbenchConfig.billing.bufferCapacityPerTier =
        ImmutableMap.of(registeredTier.getShortName(), 1, controlledTier.getShortName(), 1);

    billingProjectBufferService.bufferBillingProjects();

    Set<String> expectedPerimeters =
        Sets.asSet(registeredTier.getServicePerimeter(), controlledTier.getServicePerimeter());

    // one project per tier, per bufferRefillProjectsPerTask
    final int expectedCreationCount =
        workbenchConfig.billing.bufferRefillProjectsPerTask
            * workbenchConfig.billing.bufferCapacityPerTier.size();

    ArgumentCaptor<String> projectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> perimeterCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService, times(expectedCreationCount))
        .createAllOfUsBillingProject(projectCaptor.capture(), perimeterCaptor.capture());

    for (String billingProjectName : projectCaptor.getAllValues()) {
      assertThat(billingProjectName).startsWith(workbenchConfig.billing.projectNamePrefix);
      assertThat(
              billingProjectBufferEntryDao
                  .findByFireCloudProjectName(billingProjectName)
                  .getStatusEnum())
          .isEqualTo(BufferEntryStatus.CREATING);
    }

    assertThat(perimeterCaptor.getAllValues()).containsExactlyElementsIn(expectedPerimeters);
  }

  @Test
  public void fillBuffer_failedCreateRequest() {
    long expectedCount = billingProjectBufferEntryDao.count() + 1;

    doThrow(RuntimeException.class)
        .when(mockFireCloudService)
        .createAllOfUsBillingProject(anyString(), anyString());
    try {
      billingProjectBufferService.bufferBillingProjects();
    } catch (Exception e) {
    }

    assertThat(billingProjectBufferEntryDao.count()).isEqualTo(expectedCount);
  }

  @Test
  public void fillBuffer_prefixName() {
    workbenchConfig.billing.projectNamePrefix = "test-prefix-";
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService).createAllOfUsBillingProject(captor.capture(), anyString());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).doesNotContain("--");
  }

  @Test
  public void fillBuffer_capacity() {
    // fill up buffer
    for (int i = 0; i < REGISTERED_TIER_BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProjects();
    }
    int expectedCallCount = 0;
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString(), anyString());

    // free up buffer
    DbBillingProjectBufferEntry entry = billingProjectBufferEntryDao.findAll().iterator().next();
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    expectedCallCount++;
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString(), anyString());

    // increase buffer capacity
    expectedCallCount++;
    workbenchConfig.billing.bufferCapacityPerTier =
        Collections.singletonMap(
            registeredTier.getShortName(), REGISTERED_TIER_BUFFER_CAPACITY + 1);
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString(), anyString());

    // should be at capacity
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString(), anyString());
  }

  @Test
  public void fillBuffer_decreaseCapacity() {
    // fill up buffer
    for (int i = 0; i < REGISTERED_TIER_BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProjects();
    }

    workbenchConfig.billing.bufferCapacityPerTier =
        Collections.singletonMap(
            registeredTier.getShortName(), REGISTERED_TIER_BUFFER_CAPACITY - 2);

    // should no op since we're at capacity + 2
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY))
        .createAllOfUsBillingProject(anyString(), anyString());

    // should no op since we're at capacity + 1
    Iterator<DbBillingProjectBufferEntry> bufferEntries =
        billingProjectBufferEntryDao.findAll().iterator();
    DbBillingProjectBufferEntry entry = bufferEntries.next();
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY))
        .createAllOfUsBillingProject(anyString(), anyString());

    // should no op since we're at capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY))
        .createAllOfUsBillingProject(anyString(), anyString());

    // should invoke since we're below capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(BufferEntryStatus.ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(mockFireCloudService, times((int) REGISTERED_TIER_BUFFER_CAPACITY + 1))
        .createAllOfUsBillingProject(anyString(), anyString());
  }

  @Test
  public void syncBillingProjectStatus_creating() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService)
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    String billingProjectName = captor.getValue();

    FirecloudBillingProjectStatus billingProjectStatus = new FirecloudBillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.CREATING);
    doReturn(billingProjectStatus)
        .when(mockFireCloudService)
        .getBillingProjectStatus(billingProjectName);

    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);

    billingProjectStatus.setCreationStatus(CreationStatusEnum.ADDINGTOPERIMETER);
    doReturn(billingProjectStatus)
        .when(mockFireCloudService)
        .getBillingProjectStatus(billingProjectName);

    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);
  }

  @Test
  public void syncBillingProjectStatus_ready() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService)
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    String billingProjectName = captor.getValue();

    FirecloudBillingProjectStatus billingProjectStatus = new FirecloudBillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.READY);
    doReturn(billingProjectStatus)
        .when(mockFireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.AVAILABLE);
  }

  @Test
  public void syncBillingProjectStatus_error() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService)
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    String billingProjectName = captor.getValue();

    FirecloudBillingProjectStatus billingProjectStatus =
        new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.ERROR);
    doReturn(billingProjectStatus)
        .when(mockFireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.ERROR);
  }

  @Test
  public void syncBillingProjectStatus_notFound() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService)
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    String billingProjectName = captor.getValue();

    doThrow(NotFoundException.class)
        .when(mockFireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);
  }

  @Test
  public void syncBillingProjectStatus_multiple() {
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService, times(3))
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.ERROR))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(countByStatus(BufferEntryStatus.CREATING)).isEqualTo(0);
    assertThat(countByStatus(BufferEntryStatus.AVAILABLE)).isEqualTo(2);
    assertThat(countByStatus(BufferEntryStatus.ERROR)).isEqualTo(1);
  }

  private long countByStatus(BufferEntryStatus status) {
    return StreamSupport.stream(billingProjectBufferEntryDao.findAll().spliterator(), false)
        .filter(entry -> entry.getStatusEnum() == status)
        .count();
  }

  @Test
  public void syncBillingProjectStatus_iteratePastStallingRequests() {
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService, times(3))
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doThrow(WorkbenchException.class)
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(capturedProjectNames.get(2))
                .getStatusEnum())
        .isEqualTo(BufferEntryStatus.AVAILABLE);
  }

  @Test
  public void testOutageRecoveryTime() {
    // This test runs through a simulated outage-and-recovery scenario, checking the time
    // it takes Workbench to recover with a project in the READY state after an extended
    // Terra outage.
    //
    // See RW-6192 for more context on the issue that motivated this test. When that issue
    // was filed, it took a 300-project buffer ~1-2 hours to reach a recovery state. This
    // test demonstrates that with an appropriate set of configuration values, a 300-project
    // buffer can recover almost immediately after the first non-error project is ready.

    workbenchConfig.billing.bufferCapacityPerTier =
        Collections.singletonMap(registeredTier.getShortName(), 300);
    workbenchConfig.billing.bufferRefillProjectsPerTask = 5;
    workbenchConfig.billing.bufferStatusChecksPerTask = 10;

    // Set up the core Terra mock. All projects will return CREATING status for 15 minutes after
    // their creation time. After 15 minutes, they will return ERROR by default, or SUCCESS if
    // the project name has been added to the HashSet.
    Set<String> successProjectNames = new HashSet<>();
    doAnswer(
            invocation -> {
              String projectName = invocation.getArgument(0);
              DbBillingProjectBufferEntry entry =
                  billingProjectBufferEntryDao.findByFireCloudProjectName(projectName);
              Duration timeSinceCreation =
                  Duration.between(entry.getCreationTime().toInstant(), CLOCK.instant());
              log.fine(
                  String.format(
                      "%s is %s mins old (%s project)",
                      projectName,
                      timeSinceCreation.toMinutes(),
                      successProjectNames.contains(projectName) ? "SUCCESS" : "ERROR"));
              CreationStatusEnum status = null;
              if (timeSinceCreation.toMinutes() < 15) {
                status = CreationStatusEnum.CREATING;
              } else if (successProjectNames.contains(projectName)) {
                status = CreationStatusEnum.READY;
              } else {
                status = CreationStatusEnum.ERROR;
              }
              return new FirecloudBillingProjectStatus()
                  .creationStatus(status)
                  .projectName(projectName);
            })
        .when(mockFireCloudService)
        .getBillingProjectStatus(anyString());

    // This lambda simulates the passage of time, with the clock incrementing and each of the
    // buffer and sync cron jobs executing every minute.
    Runnable tick =
        () -> {
          CLOCK.setInstant(CLOCK.instant().plus(Duration.ofMinutes(1)));
          billingProjectBufferService.bufferBillingProjects();
          billingProjectBufferService.syncBillingProjectStatus();
        };

    Instant startingTime = CLOCK.instant();

    // Simulate a Terra outage: create a bunch of projects which will error-out after 15 minutes.
    while (billingProjectBufferEntryDao.count() < 300) {
      tick.run();
      if (Duration.between(startingTime, CLOCK.instant()).toHours() > 3) {
        // Bail out if it takes too long
        break;
      }
    }

    List<ProjectCountByStatusAndTier> projectCounts =
        billingProjectBufferEntryDao.getBillingBufferGaugeData();

    // Sanity check: there shouldn't be any AVAILABLE projects.
    assertThat(filterByStatus(projectCounts, BufferEntryStatus.AVAILABLE)).isEmpty();

    // Sanity check: there should be non-zero CREATING projects.
    assertThat(filterByStatus(projectCounts, BufferEntryStatus.CREATING)).isNotEmpty();

    // Simulate a Terra system recovery.
    //
    // Add a new mock to the buffer entry DAO, which will take any newly-created buffer entries
    // and add them to the successProjectNames list, so they will become SUCCESS status after 15
    // minutes.
    doAnswer(
            invocation -> {
              DbBillingProjectBufferEntry entry = invocation.getArgument(0);
              // Use a nonexistent ID as a signal that the current entry is being newly-created.
              if (entry.getId() == 0) {
                successProjectNames.add(entry.getFireCloudProjectName());
              }
              return invocation.callRealMethod();
            })
        .when(billingProjectBufferEntryDao)
        .save((DbBillingProjectBufferEntry) any());

    Instant terraRecoveryTime = CLOCK.instant();
    Instant workbenchRecoveryTime = null;
    while (true) {
      tick.run();

      int availableCount =
          filterByStatus(
                  billingProjectBufferEntryDao.getBillingBufferGaugeData(),
                  BufferEntryStatus.AVAILABLE)
              .size();

      // Recovery is defined as "time to first project available"
      if (availableCount >= 1) {
        workbenchRecoveryTime = CLOCK.instant();
        break;
      }
      if (Duration.between(startingTime, CLOCK.instant()).toHours() > 4) {
        // To avoid looping forever, bail out after 4 "hours" if this keeps running.
        log.severe("Took too long recovering, bailing out");
        break;
      }
    }

    assertThat(workbenchRecoveryTime).isNotNull();
    long workbenchRecoveryMinutes =
        Duration.between(terraRecoveryTime, workbenchRecoveryTime).toMinutes();
    log.info(String.format("Workbench recovered in %s minutes", workbenchRecoveryMinutes));
    assertThat(workbenchRecoveryMinutes).isLessThan(30l);
  }

  private List<ProjectCountByStatusAndTier> filterByStatus(
      List<ProjectCountByStatusAndTier> projectCounts, BufferEntryStatus status) {
    return projectCounts.stream()
        .filter(c -> c.getStatusEnum().equals(status))
        .collect(Collectors.toList());
  }

  @Test
  public void assignBillingProject() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
    entry.setFireCloudProjectName("test-project-name");
    entry.setCreationTime(getCurrentTimestamp());
    entry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(entry);

    DbUser user = mock(DbUser.class);
    doReturn("fake-email@aou.org").when(user).getUsername();

    DbBillingProjectBufferEntry assignedEntry =
        billingProjectBufferService.assignBillingProject(user, registeredTier);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> secondCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService).addOwnerToBillingProject(captor.capture(), secondCaptor.capture());
    String invokedEmail = captor.getValue();
    String invokedProjectName = secondCaptor.getValue();

    assertThat(invokedEmail).isEqualTo("fake-email@aou.org");
    assertThat(invokedProjectName).isEqualTo("test-project-name");

    assertThat(billingProjectBufferEntryDao.findById(assignedEntry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ASSIGNED);
  }

  @Test
  public void assignBillingProjectMultiTier() {
    DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
    entry.setFireCloudProjectName("test-project-name");
    entry.setCreationTime(getCurrentTimestamp());
    entry.setAccessTier(controlledTier);
    billingProjectBufferEntryDao.save(entry);

    DbUser user = mock(DbUser.class);
    doReturn("fake-email@aou.org").when(user).getUsername();

    DbBillingProjectBufferEntry assignedEntry =
        billingProjectBufferService.assignBillingProject(user, controlledTier);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> secondCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService).addOwnerToBillingProject(captor.capture(), secondCaptor.capture());
    String invokedEmail = captor.getValue();
    String invokedProjectName = secondCaptor.getValue();

    assertThat(invokedEmail).isEqualTo("fake-email@aou.org");
    assertThat(invokedProjectName).isEqualTo("test-project-name");

    assertThat(billingProjectBufferEntryDao.findById(assignedEntry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ASSIGNED);
  }

  @Test(expected = EmptyBufferException.class)
  public void assignBillingProjectInvalidTier() {
    DbUser user = mock(DbUser.class);
    doReturn("fake-email@aou.org").when(user).getUsername();

    billingProjectBufferService.assignBillingProject(user, new DbAccessTier());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
  public void assignBillingProject_locking() throws InterruptedException, ExecutionException {
    DbBillingProjectBufferEntry firstEntry = new DbBillingProjectBufferEntry();
    firstEntry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
    firstEntry.setFireCloudProjectName("test-project-name-1");
    firstEntry.setCreationTime(getCurrentTimestamp());
    firstEntry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(firstEntry);

    DbUser firstUser = new DbUser();
    firstUser.setUsername("fake-email-1@aou.org");
    userDao.save(firstUser);

    DbBillingProjectBufferEntry secondEntry = new DbBillingProjectBufferEntry();
    secondEntry.setStatusEnum(BufferEntryStatus.AVAILABLE, this::getCurrentTimestamp);
    secondEntry.setFireCloudProjectName("test-project-name-2");
    secondEntry.setCreationTime(getCurrentTimestamp());
    secondEntry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(secondEntry);

    DbUser secondUser = new DbUser();
    secondUser.setUsername("fake-email-2@aou.org");
    userDao.save(secondUser);

    doAnswer(new CallsRealMethodsWithDelay(500))
        .when(billingProjectBufferEntryDao)
        .save(any(DbBillingProjectBufferEntry.class));

    Callable<DbBillingProjectBufferEntry> t1 =
        () -> billingProjectBufferService.assignBillingProject(firstUser, registeredTier);
    Callable<DbBillingProjectBufferEntry> t2 =
        () -> billingProjectBufferService.assignBillingProject(secondUser, registeredTier);

    List<Callable<DbBillingProjectBufferEntry>> callableTasks = new ArrayList<>();
    callableTasks.add(t1);
    callableTasks.add(t2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<Future<DbBillingProjectBufferEntry>> futures = executor.invokeAll(callableTasks);

    assertThat(futures.get(0).get().getFireCloudProjectName())
        .isNotEqualTo(futures.get(1).get().getFireCloudProjectName());
  }

  @Test
  public void cleanBillingBuffer_creating() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName("foo");
    entry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    entry.setLastSyncRequestTime(getCurrentTimestamp());
    entry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(entry);

    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(entry.getFireCloudProjectName());

    CLOCK.setInstant(AFTER_CREATING_TIMEOUT_ELAPSED);
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(billingProjectBufferEntryDao.findById(entry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ERROR);
  }

  @Test
  public void cleanBillingBuffer_creatingNeverSynced() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName("foo");
    entry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    entry.setLastSyncRequestTime(getCurrentTimestamp());
    entry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(entry);

    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(entry.getFireCloudProjectName());

    CLOCK.setInstant(AFTER_CREATING_TIMEOUT_ELAPSED);
    billingProjectBufferService.cleanBillingBuffer();

    // Time has elapsed but no status sync has occurred, should not transition.
    assertThat(billingProjectBufferEntryDao.findById(entry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);
  }

  @Test
  public void cleanBillingBuffer_creatingDelayedSync() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName("foo");
    entry.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    entry.setLastSyncRequestTime(getCurrentTimestamp());
    entry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(entry);

    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(entry.getFireCloudProjectName());
    billingProjectBufferService.syncBillingProjectStatus();

    CLOCK.setInstant(AFTER_CREATING_TIMEOUT_ELAPSED);
    billingProjectBufferService.cleanBillingBuffer();

    // Time has elapsed but no status sync has occurred, should not transition.
    assertThat(billingProjectBufferEntryDao.findById(entry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);

    // Sync again, now after the timeout - should transition.
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(billingProjectBufferEntryDao.findById(entry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ERROR);
  }

  @Test
  public void cleanBillingBuffer_assigning() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(BufferEntryStatus.ASSIGNING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    entry.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(entry);

    CLOCK.setInstant(AFTER_ASSIGNING_TIMEOUT_ELAPSED);
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findById(entry.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ERROR);
  }

  @Test
  public void cleanBillingBuffer_ignoreValidEntries() {
    DbBillingProjectBufferEntry assigning = new DbBillingProjectBufferEntry();
    assigning.setStatusEnum(BufferEntryStatus.ASSIGNING, this::getCurrentTimestamp);
    assigning.setCreationTime(getCurrentTimestamp());
    assigning.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(assigning);

    CLOCK.setInstant(BEFORE_ASSIGNING_TIMEOUT_ELAPSES);
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findById(assigning.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.ASSIGNING);

    DbBillingProjectBufferEntry creating = new DbBillingProjectBufferEntry();
    creating.setStatusEnum(BufferEntryStatus.CREATING, this::getCurrentTimestamp);
    creating.setCreationTime(getCurrentTimestamp());
    creating.setAccessTier(registeredTier);
    billingProjectBufferEntryDao.save(creating);

    CLOCK.setInstant(BEFORE_CREATING_TIMEOUT_ELAPSES);
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findById(creating.getId()).get().getStatusEnum())
        .isEqualTo(BufferEntryStatus.CREATING);
  }

  @Test
  public void cleanBillingBuffer_multipleCreates() {
    billingProjectBufferService.bufferBillingProjects();

    CLOCK.setInstant(NOW.plus(30, ChronoUnit.MINUTES));
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();

    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(mockFireCloudService, times(3))
        .createAllOfUsBillingProject(captor.capture(), eq(registeredTier.getServicePerimeter()));
    final List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new FirecloudBillingProjectStatus().creationStatus(CreationStatusEnum.ERROR))
        .when(mockFireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    // This lambda yields the respective buffer statuses of the captured test projects.
    Supplier<List<BufferEntryStatus>> statusSupplier =
        () ->
            capturedProjectNames.stream()
                .map(billingProjectBufferEntryDao::findByFireCloudProjectName)
                .map(DbBillingProjectBufferEntry::getStatusEnum)
                .collect(Collectors.toList());

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(statusSupplier.get())
        .containsExactly(
            BufferEntryStatus.CREATING, BufferEntryStatus.CREATING, BufferEntryStatus.ERROR);

    // Time elapses, but no sync yet.
    CLOCK.setInstant(AFTER_CREATING_TIMEOUT_ELAPSED);
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(statusSupplier.get())
        .containsExactly(
            BufferEntryStatus.CREATING, BufferEntryStatus.CREATING, BufferEntryStatus.ERROR);

    // Sync expires project 1, project 2 is still pending.
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(statusSupplier.get())
        .containsExactly(
            BufferEntryStatus.ERROR, BufferEntryStatus.CREATING, BufferEntryStatus.ERROR);

    // Finally, expire project 2 as well.
    CLOCK.setInstant(AFTER_CREATING_TIMEOUT_ELAPSED.plus(30, ChronoUnit.MINUTES));
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.cleanBillingBuffer();
    assertThat(statusSupplier.get())
        .containsExactly(BufferEntryStatus.ERROR, BufferEntryStatus.ERROR, BufferEntryStatus.ERROR);
  }

  @Test
  public void testGetStatus() {
    DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    assertThat(
            billingProjectBufferService.getStatus().getAvailablePerTier().stream()
                .allMatch(tier -> tier.getAvailableProjects() == 0))
        .isTrue();

    makeEntry(BufferEntryStatus.AVAILABLE, registeredTier);
    makeEntry(BufferEntryStatus.AVAILABLE, registeredTier);
    makeEntry(BufferEntryStatus.AVAILABLE, registeredTier);

    makeEntry(BufferEntryStatus.AVAILABLE, controlledTier);
    makeEntry(BufferEntryStatus.AVAILABLE, controlledTier);

    // these are not counted

    makeEntry(BufferEntryStatus.ERROR, registeredTier);
    makeEntry(BufferEntryStatus.ASSIGNED, controlledTier);
    makeEntry(BufferEntryStatus.ASSIGNING, registeredTier);
    makeEntry(BufferEntryStatus.CREATING, controlledTier);
    makeEntry(BufferEntryStatus.GARBAGE_COLLECTED, registeredTier);
    makeEntry(BufferEntryStatus.GARBAGE_COLLECTED, controlledTier);

    BillingProjectBufferStatus status = billingProjectBufferService.getStatus();
    List<AvailableBufferPerTier> perTier = status.getAvailablePerTier();
    assertThat(perTier).hasSize(2);

    Optional<Long> registeredAvailable =
        perTier.stream()
            .filter(tier -> tier.getAccessTierShortName().equals(registeredTier.getShortName()))
            .map(AvailableBufferPerTier::getAvailableProjects)
            .findAny();
    assertThat(registeredAvailable).hasValue(3);

    Optional<Long> controlledAvailable =
        perTier.stream()
            .filter(tier -> tier.getAccessTierShortName().equals(controlledTier.getShortName()))
            .map(AvailableBufferPerTier::getAvailableProjects)
            .findAny();
    assertThat(controlledAvailable).hasValue(2);
  }

  @Test
  public void testFindEntriesWithExpiredGracePeriod() {

    // populate a variety of entries, including 2 which are older than the Creating grace period
    // and 3 which have passed the Assigning grace period

    Instant baseTime = NOW.minus(Period.ofDays(1));
    Instant testTime = baseTime.plus(Duration.ofMinutes(100));

    // past grace period of 60 minutes ago
    makeEntry(BufferEntryStatus.CREATING, testTime.minus(Duration.ofMinutes(90)));
    makeEntry(BufferEntryStatus.CREATING, testTime.minus(Duration.ofMinutes(70)));

    // within grace period
    makeEntry(BufferEntryStatus.CREATING, testTime.minus(Duration.ofMinutes(40)));

    // past grace period of 10 minutes ago
    makeEntry(BufferEntryStatus.ASSIGNING, testTime.minus(Duration.ofMinutes(35)));
    makeEntry(BufferEntryStatus.ASSIGNING, testTime.minus(Duration.ofMinutes(25)));
    makeEntry(BufferEntryStatus.ASSIGNING, testTime.minus(Duration.ofMinutes(15)));

    // within grace period
    makeEntry(BufferEntryStatus.ASSIGNING, testTime.minus(Duration.ofMinutes(5)));

    // other irrelevant entries
    makeEntry(BufferEntryStatus.ERROR, baseTime);
    makeEntry(BufferEntryStatus.GARBAGE_COLLECTED, baseTime.plus(Duration.ofMinutes(20)));
    makeEntry(BufferEntryStatus.AVAILABLE, baseTime.plus(Duration.ofMinutes(99)));

    List<Timestamp> expectedExpiredCreating =
        Lists.newArrayList(
            Timestamp.from(testTime.minus(Duration.ofMinutes(90))),
            Timestamp.from(testTime.minus(Duration.ofMinutes(70))));

    List<Timestamp> observedExpiredCreating =
        billingProjectBufferService
            .findEntriesWithExpiredGracePeriod(testTime, BufferEntryStatus.CREATING).stream()
            .map(DbBillingProjectBufferEntry::getLastSyncRequestTime)
            .collect(Collectors.toList());

    assertThat(observedExpiredCreating).containsExactlyElementsIn(expectedExpiredCreating);

    List<Timestamp> expectedExpiredAssigning =
        Lists.newArrayList(
            Timestamp.from(testTime.minus(Duration.ofMinutes(35))),
            Timestamp.from(testTime.minus(Duration.ofMinutes(25))),
            Timestamp.from(testTime.minus(Duration.ofMinutes(15))));

    List<Timestamp> observedExpiredAssigning =
        billingProjectBufferService
            .findEntriesWithExpiredGracePeriod(testTime, BufferEntryStatus.ASSIGNING).stream()
            .map(DbBillingProjectBufferEntry::getLastSyncRequestTime)
            .collect(Collectors.toList());

    assertThat(observedExpiredAssigning).containsExactlyElementsIn(expectedExpiredAssigning);
  }

  @Test
  public void createBillingProjectName_withUnderScore() {
    String prefix = "prefix-";
    workbenchConfig.billing.projectNamePrefix = prefix;
    String projectName = billingProjectBufferService.createBillingProjectName();

    assertThat(projectName.startsWith(prefix)).isTrue();
    assertThat(projectName.length()).isEqualTo(prefix.length() + PROJECT_BILLING_ID_SIZE);
  }

  @Test
  public void createBillingProjectName_withoutUnderScore() {
    String prefix = "prefix";
    workbenchConfig.billing.projectNamePrefix = prefix;
    String projectName = billingProjectBufferService.createBillingProjectName();

    assertThat(projectName.startsWith(prefix + "-")).isTrue();
    assertThat(projectName.length()).isEqualTo(prefix.length() + 1 + PROJECT_BILLING_ID_SIZE);
  }

  private DbBillingProjectBufferEntry makeEntry(BufferEntryStatus status, Instant lastUpdatedTime) {
    return makeEntry(status, lastUpdatedTime, registeredTier);
  }

  private DbBillingProjectBufferEntry makeEntry(BufferEntryStatus status, DbAccessTier accessTier) {
    return makeEntry(status, CLOCK.instant(), accessTier);
  }

  private DbBillingProjectBufferEntry makeEntry(
      BufferEntryStatus status, Instant lastUpdatedTime, DbAccessTier accessTier) {
    final DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(status, () -> Timestamp.from(lastUpdatedTime));
    entry.setLastSyncRequestTime(Timestamp.from(lastUpdatedTime));
    entry.setAccessTier(accessTier);
    return billingProjectBufferEntryDao.save(entry);
  }

  private Timestamp getCurrentTimestamp() {
    return new Timestamp(NOW.toEpochMilli());
  }
}
