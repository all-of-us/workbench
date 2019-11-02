package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNED;
import static org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNING;
import static org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE;
import static org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus.CREATING;
import static org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BillingProjectBufferStatus.ERROR;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.CallsRealMethodsWithDelay;
import org.pmiops.workbench.TestLock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.BillingProjectStatus;
import org.pmiops.workbench.firecloud.model.BillingProjectStatus.CreationStatusEnum;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class BillingProjectBufferServiceTest {

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({BillingProjectBufferService.class})
  @MockBean({FireCloudService.class})
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
  @Autowired private BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  @Autowired private FireCloudService fireCloudService;

  @Autowired private BillingProjectBufferService billingProjectBufferService;

  private final long BUFFER_CAPACITY = 5;

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.projectNamePrefix = "test-prefix";
    workbenchConfig.billing.bufferCapacity = (int) BUFFER_CAPACITY;
    workbenchConfig.billing.bufferRefillProjectsPerTask = 1;

    billingProjectBufferEntryDao = spy(billingProjectBufferEntryDao);
    TestLock lock = new TestLock();
    doAnswer(invocation -> lock.lock()).when(billingProjectBufferEntryDao).acquireAssigningLock();
    doAnswer(invocation -> lock.release())
        .when(billingProjectBufferEntryDao)
        .releaseAssigningLock();

    billingProjectBufferService =
        new BillingProjectBufferService(
            billingProjectBufferEntryDao, clock, fireCloudService, workbenchConfigProvider);
  }

  @Test
  public void canBufferMultipleProjectsPerTask() {
    workbenchConfig.billing.bufferRefillProjectsPerTask = 2;
    billingProjectBufferService.bufferBillingProjects();

    verify(fireCloudService, times(2)).createAllOfUsBillingProject(anyString());
  }

  @Test
  public void fillBuffer() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).startsWith(workbenchConfig.billing.projectNamePrefix);
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(CREATING);
  }

  @Test
  public void fillBuffer_failedCreateRequest() {
    long expectedCount = billingProjectBufferEntryDao.count() + 1;

    doThrow(RuntimeException.class).when(fireCloudService).createAllOfUsBillingProject(anyString());
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
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).doesNotContain("--");
  }

  @Test
  public void fillBuffer_capacity() {
    // fill up buffer
    for (int i = 0; i < BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProjects();
    }
    int expectedCallCount = 0;
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // free up buffer
    DbBillingProjectBufferEntry entry = billingProjectBufferEntryDao.findAll().iterator().next();
    entry.setStatusEnum(ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    expectedCallCount++;
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // increase buffer capacity
    expectedCallCount++;
    workbenchConfig.billing.bufferCapacity = (int) BUFFER_CAPACITY + 1;
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // should be at capacity
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());
  }

  @Test
  public void fillBuffer_decreaseCapacity() {
    // fill up buffer
    for (int i = 0; i < BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProjects();
    }

    workbenchConfig.billing.bufferCapacity = (int) BUFFER_CAPACITY - 2;

    // should no op since we're at capacity + 2
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY)).createAllOfUsBillingProject(anyString());

    // should no op since we're at capacity + 1
    Iterator<DbBillingProjectBufferEntry> bufferEntries =
        billingProjectBufferEntryDao.findAll().iterator();
    DbBillingProjectBufferEntry entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY)).createAllOfUsBillingProject(anyString());

    // should no op since we're at capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY)).createAllOfUsBillingProject(anyString());

    // should invoke since we're below capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED, this::getCurrentTimestamp);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProjects();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + 1))
        .createAllOfUsBillingProject(anyString());
  }

  @Test
  public void syncBillingProjectStatus_creating() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus = new BillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.CREATING);
    doReturn(billingProjectStatus)
        .when(fireCloudService)
        .getBillingProjectStatus(billingProjectName);

    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(CREATING);

    billingProjectStatus.setCreationStatus(CreationStatusEnum.ADDINGTOPERIMETER);
    doReturn(billingProjectStatus)
        .when(fireCloudService)
        .getBillingProjectStatus(billingProjectName);

    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(CREATING);
  }

  @Test
  public void syncBillingProjectStatus_ready() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus = new BillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.READY);
    doReturn(billingProjectStatus)
        .when(fireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(AVAILABLE);
  }

  @Test
  public void syncBillingProjectStatus_error() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus =
        new BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR);
    doReturn(billingProjectStatus)
        .when(fireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(ERROR);
  }

  @Test
  public void syncBillingProjectStatus_notFound() {
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    doThrow(NotFoundException.class)
        .when(fireCloudService)
        .getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(billingProjectName)
                .getStatusEnum())
        .isEqualTo(CREATING);
  }

  @Test
  public void syncBillingProjectStatus_multiple() {
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture());
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR))
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(
            billingProjectBufferEntryDao.countByStatus(
                StorageEnums.billingProjectBufferStatusToStorage(CREATING)))
        .isEqualTo(0);
    assertThat(
            billingProjectBufferEntryDao.countByStatus(
                StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE)))
        .isEqualTo(2);
    assertThat(
            billingProjectBufferEntryDao.countByStatus(
                StorageEnums.billingProjectBufferStatusToStorage(ERROR)))
        .isEqualTo(1);
  }

  @Test
  public void syncBillingProjectStatus_iteratePastStallingRequests() {
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();
    billingProjectBufferService.bufferBillingProjects();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture());
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.CREATING))
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doThrow(WorkbenchException.class)
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY))
        .when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(
            billingProjectBufferEntryDao
                .findByFireCloudProjectName(capturedProjectNames.get(2))
                .getStatusEnum())
        .isEqualTo(AVAILABLE);
  }

  @Test
  public void assignBillingProject() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(AVAILABLE, this::getCurrentTimestamp);
    entry.setFireCloudProjectName("test-project-name");
    entry.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(entry);

    User user = mock(User.class);
    doReturn("fake-email@aou.org").when(user).getEmail();

    DbBillingProjectBufferEntry assignedEntry =
        billingProjectBufferService.assignBillingProject(user);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> secondCaptor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).addUserToBillingProject(captor.capture(), secondCaptor.capture());
    String invokedEmail = captor.getValue();
    String invokedProjectName = secondCaptor.getValue();

    assertThat(invokedEmail).isEqualTo("fake-email@aou.org");
    assertThat(invokedProjectName).isEqualTo("test-project-name");

    assertThat(billingProjectBufferEntryDao.findOne(assignedEntry.getId()).getStatusEnum())
        .isEqualTo(ASSIGNED);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
  public void assignBillingProject_locking() throws InterruptedException, ExecutionException {
    DbBillingProjectBufferEntry firstEntry = new DbBillingProjectBufferEntry();
    firstEntry.setStatusEnum(AVAILABLE, this::getCurrentTimestamp);
    firstEntry.setFireCloudProjectName("test-project-name-1");
    firstEntry.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(firstEntry);

    User firstUser = new User();
    firstUser.setEmail("fake-email-1@aou.org");
    userDao.save(firstUser);

    DbBillingProjectBufferEntry secondEntry = new DbBillingProjectBufferEntry();
    secondEntry.setStatusEnum(AVAILABLE, this::getCurrentTimestamp);
    secondEntry.setFireCloudProjectName("test-project-name-2");
    secondEntry.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(secondEntry);

    User secondUser = new User();
    secondUser.setEmail("fake-email-2@aou.org");
    userDao.save(secondUser);

    doAnswer(new CallsRealMethodsWithDelay(500))
        .when(billingProjectBufferEntryDao)
        .save(any(DbBillingProjectBufferEntry.class));

    Callable<DbBillingProjectBufferEntry> t1 =
        () -> billingProjectBufferService.assignBillingProject(firstUser);
    Callable<DbBillingProjectBufferEntry> t2 =
        () -> billingProjectBufferService.assignBillingProject(secondUser);

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
    entry.setStatusEnum(CREATING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(entry);

    CLOCK.setInstant(NOW.plus(61, ChronoUnit.MINUTES));
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findOne(entry.getId()).getStatusEnum())
        .isEqualTo(ERROR);
  }

  @Test
  public void cleanBillingBuffer_assigning() {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setStatusEnum(ASSIGNING, this::getCurrentTimestamp);
    entry.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(entry);

    CLOCK.setInstant(NOW.plus(11, ChronoUnit.MINUTES));
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findOne(entry.getId()).getStatusEnum())
        .isEqualTo(ERROR);
  }

  @Test
  public void cleanBillingBuffer_ignoreValidEntries() {
    DbBillingProjectBufferEntry creating = new DbBillingProjectBufferEntry();
    creating.setStatusEnum(CREATING, this::getCurrentTimestamp);
    creating.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(creating);

    CLOCK.setInstant(NOW.plus(59, ChronoUnit.MINUTES));
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findOne(creating.getId()).getStatusEnum())
        .isEqualTo(CREATING);

    DbBillingProjectBufferEntry assigning = new DbBillingProjectBufferEntry();
    assigning.setStatusEnum(ASSIGNING, this::getCurrentTimestamp);
    assigning.setCreationTime(getCurrentTimestamp());
    billingProjectBufferEntryDao.save(assigning);

    CLOCK.setInstant(NOW.plus(9, ChronoUnit.MINUTES));
    billingProjectBufferService.cleanBillingBuffer();

    assertThat(billingProjectBufferEntryDao.findOne(assigning.getId()).getStatusEnum())
        .isEqualTo(ASSIGNING);
  }

  private Timestamp getCurrentTimestamp() {
    return new Timestamp(NOW.toEpochMilli());
  }
}
