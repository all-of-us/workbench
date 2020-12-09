package org.pmiops.workbench.reporting;

import java.time.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ReportingMapperTest {
  @Autowired ReportingMapper reportingMapper;

  @Autowired
  ReportingTestFixture<DbUser, ProjectedReportingUser, ReportingUser> reportingUserFixture;

  @TestConfiguration
  @Import({CommonMappers.class, ReportingMapperImpl.class, ReportingTestConfig.class})
  @MockBean({Clock.class})
  public static class conifg {}

  @Test
  public void testToReportingCohort() {
    final ProjectedReportingCohort projectedReportingCohort =
        ReportingTestUtils.mockProjectedReportingCohort();
    final ReportingCohort cohort = reportingMapper.toReportingCohort(projectedReportingCohort);
    ReportingTestUtils.assertCohortFields(cohort);
  }

  @Test
  public void testToReportingUser() {
    final ProjectedReportingUser prjUser = reportingUserFixture.mockProjection();
    final ReportingUser dtoUser = reportingMapper.toReportingUser(prjUser);
    reportingUserFixture.assertDTOFieldsMatchConstants(dtoUser);
  }

  @Test
  public void testToReportingWorkspace() {
    final ProjectedReportingWorkspace prjWorkspace = ReportingTestUtils.mockProjectedWorkspace();
    final ReportingWorkspace bqDtoWorkspace = reportingMapper.toReportingWorkspace(prjWorkspace);
    ReportingTestUtils.assertDtoWorkspaceFields(bqDtoWorkspace);
  }
}
