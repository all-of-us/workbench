package org.pmiops.workbench.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.cloud.bigquery.TableResult;
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class BillingAlertsServiceTest {

  @Autowired BigQueryService bigQueryService;

  @Autowired
  BillingAlertsService billingAlertsService;

  @TestConfiguration
  @Import({
      BillingAlertsService.class
  })
  @MockBean({BigQueryService.class})
  static class Configuration { }

  @Before
  public void setUp() throws Exception {
    InputStream inputStream = getClass()
        .getClassLoader().getResourceAsStream("bigquery/get_billing_project_costs.ser");
    TableResult tableResult = (TableResult) (new ObjectInputStream(inputStream)).readObject();
    doReturn(tableResult).when(bigQueryService).executeQuery(any());
  }

  @Test
  public void alertUsersExceedingFreeTierBilling() {
    billingAlertsService.alertUsersExceedingFreeTierBilling();
  }
}
