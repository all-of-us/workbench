package org.pmiops.workbench.testconfig;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@TestConfiguration
public class ReportingTestConfig {

  @Bean
  public WorkbenchConfig workbenchConfig() {
    final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.reporting.dataset = "wb_reporting";
    workbenchConfig.reporting.maxRowsPerInsert = 2;
    workbenchConfig.server.projectId = "rw-wb-unit-test";
    return workbenchConfig;
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public Stopwatch getStopwatch() {
    return Stopwatch.createUnstarted(new FakeTicker(TimeUnit.MILLISECONDS.toNanos(250)));
  }

  @Bean
  @Primary
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReportingTestFixture<DbUser, ReportingUser> getUserTestFixture() {
    return new ReportingUserFixture();
  }
}
