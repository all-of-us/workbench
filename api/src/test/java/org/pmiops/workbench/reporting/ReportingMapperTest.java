package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.util.List;
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
  public void testProjectedReportingWorkspace_toDto() {
    final ProjectedReportingWorkspace prjWorkspace = ReportingTestUtils.mockProjectedWorkspace();
    final ReportingWorkspace bqDtoWorkspace = reportingMapper.toDto(prjWorkspace);
    ReportingTestUtils.assertDtoWorkspaceFields(bqDtoWorkspace);
  }

  @Test
  public void testProjectedReportingUser_toDto() {
    final ProjectedReportingUser prjUser = ReportingTestUtils.mockProjectedUser();
    final ReportingUser dtoUser = reportingMapper.toDto(prjUser);
    ReportingTestUtils.assertDtoUserFields(dtoUser);
  }

  @Test
  public void testProjectedReportingUser_manyUsers() {
    final int numUsers = 100000;
    final List<ProjectedReportingUser> users = ReportingTestUtils.createMockUserProjections(numUsers);
    final List<ReportingUser> userModels = reportingMapper.toReportingUserList(users); // no batch
    assertThat(userModels).hasSize(numUsers);
  }

  @Test
  public void testProjectedReportingUser_manyUsers_batch() {
    final int numUsers = 100000;
    final List<ProjectedReportingUser> users = ReportingTestUtils.createMockUserProjections(numUsers);
    final List<ReportingUser> userModels = reportingMapper.mapList(users, reportingMapper::toReportingUserList);
    assertThat(userModels).hasSize(numUsers);
  }

}
