package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Clock;
import java.time.Duration;
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
    final List<Integer> batchSizes = ImmutableList.of(
        1, 5, 10, 50, 100, 500, 1_000, 2000, 3000, 4000, 5_000,
        6000, 7000, 8000, 9000, 10_000, 20000, 30000, 40000, 50_000);
    final ImmutableMap.Builder<Integer, Duration> timesMapBuilder = ImmutableMap.builder();
    final Stopwatch stopwatch = Stopwatch.createUnstarted();
    for  (int batchSize :  batchSizes) {
      stopwatch.reset();
      stopwatch.start();
      final List<ProjectedReportingUser> users = ReportingTestUtils.createMockUserProjections(batchSize);
      stopwatch.stop();
//      System.out.printf("Mocked %d users in %d ms%n", batchSize, stopwatch.elapsed().toMillis());

      stopwatch.reset();
      stopwatch.start();
       final List<ReportingUser> userModels = reportingMapper.toReportingUserList(users); // no batch
      stopwatch.stop();
      timesMapBuilder.put(batchSize, stopwatch.elapsed());
//      System.out.printf("# Converted:\t%d\tDuration\t%d ms\tMemory:\t%s%n%n",
//          batchSize, stopwatch.elapsed().toMillis(), formatMemory(getUsedMemory()));
      System.out.printf("%d, %d, %d%n",
          batchSize, stopwatch.elapsed().toMillis(), getUsedMemory());

      assertThat(userModels).hasSize(batchSize);
    }
  }

  @Test
  public void testProjectedReportingUser_manyUsers_mapList() {
    final int numUsers = 100000;
    final List<ProjectedReportingUser> users = ReportingTestUtils.createMockUserProjections(numUsers);
    final List<ReportingUser> userModels = reportingMapper.mapList(users, reportingMapper::toReportingUserList);
    assertThat(userModels).hasSize(numUsers);
  }

  private long getUsedMemory() {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  private String formatMemory(long bytes) {
    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absB < 1024) {
      return bytes + " B";
    }
    long value = absB;
    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
      value >>= 10;
      ci.next();
    }
    value *= Long.signum(bytes);
    return String.format("%.1f %ciB", value / 1024.0, ci.current());
  }
}
