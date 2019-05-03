package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.pmiops.workbench.db.model.BillingProjectBufferEntry.Status.CREATING;

import java.time.Instant;
import java.time.ZoneId;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.Status;
import org.pmiops.workbench.firecloud.FireCloudService;
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
  @Import({})
  @MockBean({
      FireCloudService.class
  })
  static class Configuration {
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
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  private BillingProjectBufferService billingProjectBufferService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    billingProjectBufferService = new BillingProjectBufferService(
        billingProjectBufferEntryDao, CLOCK, fireCloudService, workbenchConfigProvider
    );

    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.firecloud = new FireCloudConfig();
    workbenchConfig.firecloud.billingProjectPrefix = "test-prefix";
    workbenchConfig.firecloud.billingProjectBufferCapacity = 10;
  }

  @Test
  public void fillBuffer() {
    billingProjectBufferService.bufferBillingProject();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fireCloudService).createAllOfUsBillingProject(captor.capture());

    String billingProjectName = captor.getValue();

    assertThat(billingProjectName).startsWith(workbenchConfig.firecloud.billingProjectPrefix);
    assertThat(billingProjectBufferEntryDao.findByProjectName(billingProjectName).getStatus())
        .isEqualTo(CREATING);
  }

  @Test
  public void fillBuffer_capacity() {
    BillingProjectBufferEntryDao dao = mock(BillingProjectBufferEntryDao.class);
    doReturn(10l).when(dao).count();
    billingProjectBufferService = new BillingProjectBufferService(dao, CLOCK, fireCloudService, workbenchConfigProvider);

    billingProjectBufferService.bufferBillingProject();
    verifyZeroInteractions(fireCloudService);

    // "free" up buffer
    doReturn(9l).when(dao).count();
    billingProjectBufferService.bufferBillingProject();
    verify(fireCloudService).createAllOfUsBillingProject(anyString());

    // reset the buffer size back to full and then increase capacity
    doReturn(10l).when(dao).count();
    workbenchConfig.firecloud.billingProjectBufferCapacity = 11;
    verify(fireCloudService).createAllOfUsBillingProject(anyString());
  }

}
