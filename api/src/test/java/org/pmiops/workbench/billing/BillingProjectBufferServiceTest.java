package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ASSIGNED;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.AVAILABLE;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.CREATING;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus.ERROR;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus;
import org.pmiops.workbench.db.model.StorageEnums;
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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class BillingProjectBufferServiceTest {

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({
      BillingProjectBufferService.class
  })
  @MockBean({
      FireCloudService.class
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

  @Autowired
  private BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  @Autowired
  private FireCloudService fireCloudService;

  @Autowired
  private BillingProjectBufferService billingProjectBufferService;

  private final long BUFFER_CAPACITY = 5;

  @Before
  public void setUp() {
    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.firecloud = new FireCloudConfig();
    workbenchConfig.firecloud.billingProjectPrefix = "test-prefix";
    workbenchConfig.firecloud.billingProjectBufferCapacity = (int) BUFFER_CAPACITY;
  }

  @Test
  public void fillBuffer() {
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).startsWith(workbenchConfig.firecloud.billingProjectPrefix);
    assertThat(billingProjectBufferEntryDao.findByFireCloudProjectName(billingProjectName).getStatusEnum())
        .isEqualTo(CREATING);
  }

  @Test
  public void fillBuffer_prefixName() {
    workbenchConfig.firecloud.billingProjectPrefix = "test-prefix-";
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).doesNotContain("--");
  }

  @Test
  public void fillBuffer_capacity() {
    // fill up buffer
    for (int i = 0; i < BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProject();
    }
    int expectedCallCount = 0;
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // free up buffer
    BillingProjectBufferEntry entry = billingProjectBufferEntryDao.findAll().iterator().next();
    entry.setStatusEnum(ASSIGNED);
    billingProjectBufferEntryDao.save(entry);
    expectedCallCount++;
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // increase buffer capacity
    expectedCallCount++;
    workbenchConfig.firecloud.billingProjectBufferCapacity = (int) BUFFER_CAPACITY + 1;
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());

    // should be at capacity
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + expectedCallCount))
        .createAllOfUsBillingProject(anyString());
  }

  @Test
  public void fillBuffer_decreaseCapacity() {
    // fill up buffer
    for (int i = 0; i < BUFFER_CAPACITY; i++) {
      billingProjectBufferService.bufferBillingProject();
    }

    workbenchConfig.firecloud.billingProjectBufferCapacity = (int) BUFFER_CAPACITY - 2;

    // should no op since we're at capacity + 2
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY))
        .createAllOfUsBillingProject(anyString());

    // should no op since we're at capacity + 1
    Iterator<BillingProjectBufferEntry> bufferEntries = billingProjectBufferEntryDao.findAll().iterator();
    BillingProjectBufferEntry entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY)).createAllOfUsBillingProject(anyString());

    // should no op since we're at capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY)).createAllOfUsBillingProject(anyString());

    // should invoke since we're below capacity
    entry = bufferEntries.next();
    entry.setStatusEnum(ASSIGNED);
    billingProjectBufferEntryDao.save(entry);
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService, times((int) BUFFER_CAPACITY + 1))
        .createAllOfUsBillingProject(anyString());
  }

  @Test
  public void syncBillingProjectStatus_creating() {
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus = new BillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.CREATING);
    doReturn(billingProjectStatus).when(fireCloudService).getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(billingProjectBufferEntryDao.findByFireCloudProjectName(billingProjectName).getStatusEnum())
        .isEqualTo(CREATING);
  }

  @Test
  public void syncBillingProjectStatus_ready() {
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus = new BillingProjectStatus();
    billingProjectStatus.setCreationStatus(CreationStatusEnum.READY);
    doReturn(billingProjectStatus).when(fireCloudService).getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(billingProjectBufferEntryDao.findByFireCloudProjectName(billingProjectName).getStatusEnum())
        .isEqualTo(BillingProjectBufferStatus.AVAILABLE);
  }

  @Test
  public void syncBillingProjectStatus_error() {
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());
    String billingProjectName = captor.getValue();

    BillingProjectStatus billingProjectStatus = new BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR);
    doReturn(billingProjectStatus).when(fireCloudService).getBillingProjectStatus(billingProjectName);
    billingProjectBufferService.syncBillingProjectStatus();
    assertThat(billingProjectBufferEntryDao.findByFireCloudProjectName(billingProjectName).getStatusEnum())
        .isEqualTo(ERROR);
  }

  @Test
  public void syncBillingProjectStatus_multiple() {
    billingProjectBufferService.bufferBillingProject();
    billingProjectBufferService.bufferBillingProject();
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture());
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY)).when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY)).when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.ERROR)).when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(billingProjectBufferEntryDao.countByStatus(StorageEnums.billingProjectBufferStatusToStorage(CREATING)))
        .isEqualTo(0);
    assertThat(billingProjectBufferEntryDao.countByStatus(StorageEnums.billingProjectBufferStatusToStorage(AVAILABLE)))
        .isEqualTo(2);
    assertThat(billingProjectBufferEntryDao.countByStatus(StorageEnums.billingProjectBufferStatusToStorage(ERROR)))
        .isEqualTo(1);
  }

  @Test
  public void syncBillingProjectStatus_iteratePastStallingRequests() {
    billingProjectBufferService.bufferBillingProject();
    billingProjectBufferService.bufferBillingProject();
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService, times(3)).createAllOfUsBillingProject(captor.capture());
    List<String> capturedProjectNames = captor.getAllValues();
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.CREATING)).when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(0));
    doThrow(RuntimeException.class).when(fireCloudService).getBillingProjectStatus(capturedProjectNames.get(1));
    doReturn(new BillingProjectStatus().creationStatus(CreationStatusEnum.READY)).when(fireCloudService)
        .getBillingProjectStatus(capturedProjectNames.get(2));

    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();
    billingProjectBufferService.syncBillingProjectStatus();

    assertThat(billingProjectBufferEntryDao.findByFireCloudProjectName(capturedProjectNames.get(2)).getStatusEnum())
        .isEqualTo(AVAILABLE);
  }
}
