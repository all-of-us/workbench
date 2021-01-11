package org.pmiops.workbench.db.jdbc;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test the unique ReportingNativeQueryService, which bypasses Spring in favor of low-level JDBC
 * queries. This means we need real DAOs.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class ReportingNativeQueryServiceTest {

  @Autowired private ReportingQueryService reportingNativeQueryService;

  // It's necessary to bring in several Dao classes, since we aim to populate join tables
  // that have neither entities of their own nor stand-alone DAOs.
  @Autowired private CdrVersionDao cCdrVersionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  @Import({ReportingQueryServiceImpl.class, ReportingUserFixture.class})
  @TestConfiguration
  public static class config {}

  @Before
  public void setup() {}

  @Test
  public void testGetReportingDatasetCohorts() {
    final DbUser user1 = createDbUser();
    final DbCdrVersion cdrVersion1 = createCdrVersion();
    final DbWorkspace workspace1 = createDbWorkspace(user1, cdrVersion1);
    final DbCohort cohort1 = createCohort(user1, workspace1);
    final DbDataset dataset1 = createDataset(workspace1, cohort1);
    entityManager.flush();

    final List<ReportingDatasetCohort> datasetCohorts =
        reportingNativeQueryService.getDatasetCohorts();
    assertThat(datasetCohorts).hasSize(1);
    assertThat(datasetCohorts.get(0).getCohortId()).isEqualTo(cohort1.getCohortId());
    assertThat(datasetCohorts.get(0).getDatasetId()).isEqualTo(dataset1.getDataSetId());
  }

  @NotNull
  @Transactional
  public DbDataset createDataset(DbWorkspace workspace1, DbCohort cohort1) {
    DbDataset dataset1 = ReportingTestUtils.createDbDataset(workspace1.getWorkspaceId());
    dataset1.setCohortIds(ImmutableList.of(cohort1.getCohortId()));
    dataset1 = dataSetDao.save(dataset1);
    assertThat(dataSetDao.count()).isEqualTo(1);
    assertThat(dataset1.getCohortIds()).containsExactly(cohort1.getCohortId());
    cohortDao.save(cohort1);
    return dataset1;
  }

  @Transactional
  public DbCohort createCohort(DbUser user1, DbWorkspace workspace1) {
    final DbCohort cohort1 = cohortDao.save(ReportingTestUtils.createDbCohort(user1, workspace1));
    assertThat(cohortDao.count()).isEqualTo(1);
    assertThat(reportingNativeQueryService.getDatasetCohorts()).isEmpty();
    return cohort1;
  }

  @Transactional
  public DbWorkspace createDbWorkspace(DbUser user1, DbCdrVersion cdrVersion1) {
    final DbWorkspace workspace1 =
        workspaceDao.save(
            ReportingTestUtils.createDbWorkspace(user1, cdrVersion1)); // save cdr version too
    assertThat(workspaceDao.count()).isEqualTo(1);
    return workspace1;
  }

  @Transactional
  public DbCdrVersion createCdrVersion() {
    DbCdrVersion cdrVersion1 = new DbCdrVersion();
    cdrVersion1.setName("foo");
    cdrVersion1 = cCdrVersionDao.save(cdrVersion1);
    assertThat(cCdrVersionDao.count()).isEqualTo(1);
    return cdrVersion1;
  }

  @Transactional
  public DbUser createDbUser() {
    final DbUser user1 = userDao.save(userFixture.createEntity());
    assertThat(userDao.count()).isEqualTo(1);
    return user1;
  }
}
