package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.reporting.ReportingServiceImpl.BATCH_UPLOADED_TABLES;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbCohort;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbNewUserSatisfactionSurvey;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbWorkspace;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryServiceImpl;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.DbGeneralDiscoverySource;
import org.pmiops.workbench.db.model.DbUser.DbPartnerDiscoverySource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingVerificationServiceTest {
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");

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

  @Autowired private ReportingVerificationService reportingVerificationService;
  @Autowired private EntityManager entityManager;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  @Autowired private UserDao userDao;
  @Autowired private CohortDao cohortDao;
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

  @BeforeEach
  public void setup() {
    final TableResult mockTableResult = mock(TableResult.class);
    doReturn(ACTUAL_COUNT_QUERY_RESULT).when(mockTableResult).getValues();

    doReturn(mockTableResult)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class));

    logCapturingStream = new ByteArrayOutputStream();
    customLogHandler = new StreamHandler(logCapturingStream, new SimpleFormatter());
    LOGGER.addHandler(customLogHandler);
  }

  @Test
  public void testVerifyBatch_verified() {
    createTableEntries(ACTUAL_COUNT);
    assertThat(
            reportingVerificationService.verifyBatchesAndLog(
                BATCH_UPLOADED_TABLES, NOW.toEpochMilli()))
        .isTrue();
    // 2 entities in database, and we uploaded 2.
    String expectedWorkspaceLogPart = "workspace\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedWorkspaceLogPart)).isTrue();
    String expectedUserLogPart = "user\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedUserLogPart)).isTrue();
    String expectedCohortLogPart = "cohort\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedCohortLogPart)).isTrue();
    String expectedNewUserSatisfactionSurveyLogPart =
        "new_user_satisfaction_survey\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedNewUserSatisfactionSurveyLogPart)).isTrue();
    String expectedUserGeneralDiscoverySourceLogPart =
        "user_general_discovery_source\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedUserGeneralDiscoverySourceLogPart)).isTrue();
    String expectedUserPartnerDiscoverySourceLogPart =
        "user_partner_discovery_source\t2\t2\t0 (0.000%)";
    assertThat(getTestCapturedLog().contains(expectedUserPartnerDiscoverySourceLogPart)).isTrue();
  }

  @Test
  public void testVerifyBatch_fail() {
    createTableEntries(ACTUAL_COUNT * 2);
    assertThat(
            reportingVerificationService.verifyBatchesAndLog(
                BATCH_UPLOADED_TABLES, NOW.toEpochMilli()))
        .isFalse();
    // 4 workspace entities, instead of the expected 2.
    String expectedWorkspaceLogPart = "workspace\t4\t2\t-2";
    assertThat(getTestCapturedLog().contains(expectedWorkspaceLogPart)).isTrue();

    // other tables do not appear in the log because we fail-fast after workspace
    String unexpectedUserLogPart = "user\t4\t2\t-2";
    assertThat(getTestCapturedLog().contains(unexpectedUserLogPart)).isFalse();
    String unexpectedCohortLogPart = "cohort\t4\t2\t-2";
    assertThat(getTestCapturedLog().contains(unexpectedCohortLogPart)).isFalse();
    String unexpectedSurveyLogPart = "new_user_satisfaction_survey\t4\t2\t-2";
    assertThat(getTestCapturedLog().contains(unexpectedSurveyLogPart)).isFalse();
  }

  /** This creates equal amount of table entries for all batch-uploaded tables. */
  private void createTableEntries(long count) {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("foo");
    cdrVersionDao.save(cdrVersion);

    for (int i = 0; i < count; ++i) {
      DbUser user = new DbUser();
      user.setGeneralDiscoverySources(ImmutableSet.of(DbGeneralDiscoverySource.OTHER_WEBSITE));
      user.setPartnerDiscoverySources(
          ImmutableSet.of(
              DbPartnerDiscoverySource.ALL_OF_US_EVENINGS_WITH_GENETICS_RESEARCH_PROGRAM));
      userDao.save(user);
      DbWorkspace dbworkspace = workspaceDao.save(createDbWorkspace(user, cdrVersion));
      cohortDao.save(createDbCohort(user, dbworkspace));
      newUserSatisfactionSurveyDao.save(createDbNewUserSatisfactionSurvey(user));
    }
    entityManager.flush();
  }

  /** Gets the captured log. */
  private static String getTestCapturedLog() {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }
}
