package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.reporting.ReportingServiceImpl.BATCH_UPLOADED_TABLES;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbWorkspace;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryServiceImpl;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingVerificationServiceTest {
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final Instant THEN_INSTANT = Instant.parse("1989-02-17T00:00:00.00Z");

  private static final Long ACTUAL_COUNT = (long) 2;
  private static final List<FieldValueList> ACTUAL_COUNT_QUERY_RESULT =
      ImmutableList.of(
          FieldValues.buildFieldValueList(
              FieldList.of(Field.of("actual_count", LegacySQLTypeName.INTEGER)),
              Arrays.asList(new Object[] {Long.toString(ACTUAL_COUNT)})));

  private static final Logger LOGGER =
      Logger.getLogger(
          ReportingVerificationServiceImpl.class
              .getName()); // matches the logger in the affected class
  private static OutputStream logCapturingStream;
  private static StreamHandler customLogHandler;

  @Autowired
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Autowired private ReportingVerificationService reportingVerificationService;
  @Autowired private EntityManager entityManager;
  @Autowired private CdrVersionDao cCdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @MockBean private BigQueryService mockBigQueryService;

  @TestConfiguration
  @Import({
    ReportingVerificationServiceImpl.class,
    ReportingTestConfig.class,
    ReportingQueryServiceImpl.class
  })
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }
  }

  @Before
  public void setup() {
    final TableResult mockTableResult = mock(TableResult.class);
    doReturn(ACTUAL_COUNT_QUERY_RESULT).when(mockTableResult).getValues();

    doReturn(mockTableResult)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class));

    logCapturingStream = new ByteArrayOutputStream();
    Handler[] handlers = LOGGER.getParent().getHandlers();
    customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
    LOGGER.addHandler(customLogHandler);
  }

  @Test
  public void testVerifyBatch_verified() throws Exception {
    createWorkspaces(ACTUAL_COUNT);
    assertThat(reportingVerificationService.verifyBatchesAndLog(BATCH_UPLOADED_TABLES, 1111111L))
        .isTrue();
    String expectedLogPart = "workspace\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedLogPart)).isTrue();
  }

  @Test
  public void testVerifyBatch_fail() throws Exception {
    createWorkspaces(ACTUAL_COUNT * 2);
    assertThat(reportingVerificationService.verifyBatchesAndLog(BATCH_UPLOADED_TABLES, 1111111L))
        .isFalse();
    String expectedLogPart = "workspace\t4\t2\t-2 (-50.000%)";
    assertThat(getTestCapturedLog().contains(expectedLogPart)).isTrue();
  }

  private void createWorkspaces(long count) {
    DbUser user = userFixture.createEntity();
    userDao.save(user);
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("foo");
    cCdrVersionDao.save(cdrVersion);

    for (int i = 0; i < count; ++i) {
      workspaceDao.save(createDbWorkspace(user, cdrVersion));
    }
    entityManager.flush();
  }

  /** Gets the captured log. */
  private static String getTestCapturedLog() throws IOException {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }
}
