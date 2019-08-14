package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
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
public class BillingAlertsServiceTest {

  @TestConfiguration
  @Import({
      BillingAlertsService.class,
      BigQueryService.class
  })
  static class Configuration {
//    @Bean
//    public Clock clock() {
//      return CLOCK;
//    }
//
//    @Bean
//    @Scope("prototype")
//    public WorkbenchConfig workbenchConfig() {
//      return workbenchConfig;
//    }
  }

  @Autowired
  BillingAlertsService billingAlertsService;

  @Test
  public void alertUsersExceedingFreeTierBilling() {
    assertThat(billingAlertsService).isNotNull();
  }
}
