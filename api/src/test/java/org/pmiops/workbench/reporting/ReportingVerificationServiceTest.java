package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbCohort;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbConceptSet;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbDataset;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbNewUserSatisfactionSurvey;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDbWorkspace;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
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
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.ReportingUploadVerificationDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryServiceImpl;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbReportingUploadVerification;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.DbGeneralDiscoverySource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.model.PartnerDiscoverySource;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingVerificationServiceTest {
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");

  private static final long ACTUAL_COUNT = 2L;
  private static final List<FieldValueList> ACTUAL_COUNT_QUERY_RESULT =
      List.of(
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
  @Autowired private CohortDao cohortDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private InstitutionDao institutionDao;
  @Autowired private NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  @Autowired private ReportingUploadVerificationDao reportingUploadVerificationDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  @MockitoBean private BigQueryService mockBigQueryService;

  @TestConfiguration
  @Import({
    ReportingQueryServiceImpl.class,
    ReportingTableService.class,
    ReportingTestConfig.class,
    ReportingVerificationServiceImpl.class,
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

  /** This creates equal amount of table entries for all batch-uploaded tables. */
  private void createTableEntries(long count) {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("foo");
    cdrVersionDao.save(cdrVersion);

    for (int i = 0; i < count; ++i) {
      institutionDao.save(
          new DbInstitution().setShortName("Inst" + i).setDisplayName("Institute #" + i));

      DbUser user =
          userDao.save(
              new DbUser()
                  .setGeneralDiscoverySources(
                      ImmutableSet.of(DbGeneralDiscoverySource.OTHER_WEBSITE))
                  .setPartnerDiscoverySources(
                      ImmutableSet.of(
                          PartnerDiscoverySource
                              .ALL_OF_US_EVENINGS_WITH_GENETICS_RESEARCH_PROGRAM)));

      newUserSatisfactionSurveyDao.save(createDbNewUserSatisfactionSurvey(user));

      DbWorkspace dbworkspace = workspaceDao.save(createDbWorkspace(user, cdrVersion));

      workspaceFreeTierUsageDao.save(
          new DbWorkspaceFreeTierUsage().setUser(user).setWorkspace(dbworkspace).setCost(i));

      DbCohort cohort = cohortDao.save(createDbCohort(user, dbworkspace));

      dataSetDao.save(
          createDbDataset(dbworkspace.getWorkspaceId())
              .setCohortIds(List.of(cohort.getCohortId()))
              .setConceptSetIds(
                  List.of(createDbConceptSet(dbworkspace.getWorkspaceId()).getConceptSetId()))
              .setValues(
                  List.of(new DbDatasetValue().setDomainId("Domain " + i).setValue("Value " + i))));
    }
    entityManager.flush();
  }

  /** Gets the captured log. */
  private static String getTestCapturedLog() {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }

  @Test
  public void testVerifyBatch_forSpecificTables_verified() {
    createTableEntries(ACTUAL_COUNT);
    var tables = List.of("cohort", "user", "workspace", "new_user_satisfaction_survey");
    var timestamp = NOW.toEpochMilli();
    tables.forEach(
        table -> reportingUploadVerificationDao.createVerificationEntry(table, timestamp));
    reportingVerificationService.verifyBatchesAndLog(tables, timestamp);

    var res = reportingUploadVerificationDao.findBySnapshotTimestamp(timestamp);
    assertThat(res.size()).isEqualTo(4);
    assertThat(res.stream().allMatch(DbReportingUploadVerification::getUploaded)).isTrue();
  }

  @Test
  public void testVerifyTablesBatch_forSpecificTables_fail() {
    createTableEntries(ACTUAL_COUNT * 2);
    var tables = List.of("cohort", "user", "workspace", "new_user_satisfaction_survey");
    var timestamp = NOW.toEpochMilli();
    tables.forEach(
        table -> reportingUploadVerificationDao.createVerificationEntry(table, timestamp));
    reportingVerificationService.verifyBatchesAndLog(
        List.of("cohort", "user", "workspace", "new_user_satisfaction_survey"), timestamp);

    var res = reportingUploadVerificationDao.findBySnapshotTimestamp(timestamp);
    assertThat(res.size()).isEqualTo(4);
    assertThat(res.stream().noneMatch(DbReportingUploadVerification::getUploaded)).isTrue();
  }

  @Test
  public void testVerifySnapshot_withAllTablesUploaded_returnsTrue() {
    // Arrange
    long timestamp = NOW.toEpochMilli();

    // Create verification records for multiple tables, all uploaded successfully
    createVerificationRecord("cohort", timestamp, true);
    createVerificationRecord("user", timestamp, true);
    createVerificationRecord("workspace", timestamp, true);

    // Act
    boolean result = reportingVerificationService.verifySnapshot(timestamp);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void testVerifySnapshot_withSomeTablesNotUploaded_returnsFalse() {
    // Arrange
    long timestamp = NOW.toEpochMilli();

    // Create verification records with mixed upload status
    createVerificationRecord("cohort", timestamp, true);
    createVerificationRecord("user", timestamp, false); // This one failed
    createVerificationRecord("workspace", timestamp, true);

    // Act
    boolean result = reportingVerificationService.verifySnapshot(timestamp);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifySnapshot_withNoTablesUploaded_returnsFalse() {
    // Arrange
    long timestamp = NOW.toEpochMilli();

    // Create verification records where none are uploaded
    createVerificationRecord("cohort", timestamp, false);
    createVerificationRecord("user", timestamp, false);

    // Act
    boolean result = reportingVerificationService.verifySnapshot(timestamp);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifySnapshot_withEmptySnapshot_returnsFalse() {
    // Arrange
    long timestamp = NOW.toEpochMilli();
    // Don't create any verification records

    // Act
    boolean result = reportingVerificationService.verifySnapshot(timestamp);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifySnapshot_withNullUploadedValues_returnsFalse() {
    // Arrange
    long timestamp = NOW.toEpochMilli();

    // Create verification records with mixed status including null
    createVerificationRecord("cohort", timestamp, true);
    createVerificationRecord("user", timestamp, null); // null uploaded value
    createVerificationRecord("workspace", timestamp, true);

    // Act
    boolean result = reportingVerificationService.verifySnapshot(timestamp);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void testVerifySnapshot_withDifferentTimestamp_returnsCorrectResult() {
    // Arrange
    long timestamp1 = NOW.toEpochMilli();
    long timestamp2 = NOW.toEpochMilli() + 1000L; // Different timestamp

    // Create records for timestamp1 (all uploaded)
    createVerificationRecord("cohort", timestamp1, true);
    createVerificationRecord("user", timestamp1, true);

    // Create records for timestamp2 (some not uploaded)
    createVerificationRecord("cohort", timestamp2, true);
    createVerificationRecord("user", timestamp2, false);

    // Act & Assert
    assertThat(reportingVerificationService.verifySnapshot(timestamp1)).isTrue();
    assertThat(reportingVerificationService.verifySnapshot(timestamp2)).isFalse();
  }

  private void createVerificationRecord(
      String tableName, Long snapshotTimestamp, Boolean uploaded) {
    DbReportingUploadVerification record = new DbReportingUploadVerification();
    record.setTableName(tableName);
    record.setSnapshotTimestamp(snapshotTimestamp);
    record.setUploaded(uploaded);
    reportingUploadVerificationDao.save(record);
    entityManager.flush();
  }
}
