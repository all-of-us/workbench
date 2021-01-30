package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.reporting.ReportingServiceImpl.BatchSupportedTableEnum.WORKSPACE;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbWorkspace;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.reporting.ReportingServiceImpl.BatchSupportedTableEnum;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
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
  private static final OffsetDateTime THEN = OffsetDateTime.ofInstant(THEN_INSTANT, ZoneOffset.UTC);

  private static Logger log =
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
  @MockBean private BigQueryService mockBigQueryService;
  @Autowired private ReportingQueryService reportingQueryService;
  @Autowired private CdrVersionDao cCdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;

  @TestConfiguration
  @Import({ReportingVerificationServiceImpl.class, ReportingTestConfig.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }
  }

  @Before
  public void setup() {
    logCapturingStream = new ByteArrayOutputStream();
    Handler[] handlers = log.getParent().getHandlers();
    customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
    log.addHandler(customLogHandler);
  }

  public String getTestCapturedLog() throws IOException {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }

  @Test
  public void testVerifyBatch_verified() throws Exception {
    createWorkspaces(2);
    Map<BatchSupportedTableEnum, Integer> map = ImmutableMap.of(WORKSPACE, 2);
    assertThat(reportingVerificationService.verifyBatchesAndLog(map, 1111111L)).isTrue();
    System.out.println("~~~~~~~");
    System.out.println(getTestCapturedLog());
  }

  @Test
  public void testVerifyBatch_fail() {
    createWorkspaces(3);
    Map<BatchSupportedTableEnum, Integer> map = ImmutableMap.of(WORKSPACE, 2);
    assertThat(reportingVerificationService.verifyBatchesAndLog(map, 1111111L)).isFalse();
  }

  private void createWorkspaces(int count) {
    DbUser user = userFixture.createEntity();
    userDao.save(user);
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("foo");
    cCdrVersionDao.save(cdrVersion);

    for (int i = 0; i < count; ++i) {
      System.out.println("11111111111111111");
      workspaceDao.save(createDbWorkspace(user, cdrVersion));
    }
    entityManager.flush();
  }
}
