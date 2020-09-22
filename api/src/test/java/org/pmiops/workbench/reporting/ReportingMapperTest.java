package org.pmiops.workbench.reporting;

import java.time.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ReportingMapperTest {
  @Autowired ReportingMapper reportingMapper;

  @TestConfiguration
  @Import({CommonMappers.class, ReportingMapperImpl.class, ReportingTestConfig.class})
  @MockBean({Clock.class})
  public static class conifg {}

  @Test
  public void testPrjWorkspace_toDto() {
    final ProjectedReportingWorkspace prjWorkspace = ReportingTestUtils.mockPrjWorkspace();
    final ReportingWorkspace bqDtoWorkspace = reportingMapper.toDto(prjWorkspace);
    ReportingTestUtils.assertDtoWorkspaceFields(bqDtoWorkspace);
  }

  @Test
  public void testPrjUser_toDto() {
    final ProjectedReportingUser prjUser = ReportingTestUtils.mockProjectedjUser();
    final ReportingUser dtoUser = reportingMapper.toDto(prjUser);
    ReportingTestUtils.assertDtoUserFields(dtoUser);
  }
}
