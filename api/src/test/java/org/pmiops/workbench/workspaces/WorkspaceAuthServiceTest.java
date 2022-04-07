package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class WorkspaceAuthServiceTest {

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private WorkspaceDao mockWorkspaceDao;

  @TestConfiguration
  @Import(WorkspaceAuthService.class)
  @MockBean(AccessTierService.class)
  static class Configuration {}

  @Test
  public void test_validateActiveBilling_active() {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubDaoGetRequired(namespace, fcName, BillingStatus.ACTIVE);

    // does not throw
    workspaceAuthService.validateActiveBilling(namespace, fcName);
  }

  @Test
  public void test_validateActiveBilling_inactive() {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubDaoGetRequired(namespace, fcName, BillingStatus.INACTIVE);

    assertThrows(
        ForbiddenException.class,
        () -> workspaceAuthService.validateActiveBilling(namespace, fcName));
  }

  private static Stream<Arguments> accessLevels() {
    return Stream.of(
        Arguments.of("OWNER", WorkspaceAccessLevel.OWNER),
        Arguments.of("WRITER", WorkspaceAccessLevel.WRITER),
        Arguments.of("READER", WorkspaceAccessLevel.READER),
        Arguments.of("NO ACCESS", WorkspaceAccessLevel.NO_ACCESS),
        Arguments.of("PROJECT_OWNER", WorkspaceAccessLevel.OWNER));
  }

  @ParameterizedTest(name = "getWorkspaceAccessLevel({0})")
  @MethodSource("accessLevels")
  public void test_getWorkspaceAccessLevel_valid(
      String accessLevel, WorkspaceAccessLevel expected) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubFcGetWorkspace(namespace, fcName, accessLevel);
    assertThat(workspaceAuthService.getWorkspaceAccessLevel(namespace, fcName)).isEqualTo(expected);
  }

  @Test
  public void test_getWorkspaceAccessLevel_invalid() {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubFcGetWorkspace(namespace, fcName, "some garbage");
    assertThrows(
        IllegalArgumentException.class,
        () -> workspaceAuthService.getWorkspaceAccessLevel(namespace, fcName));
  }

  private static Stream<Arguments> enforcedAccessLevels_valid() {
    return Stream.of(
        Arguments.of("OWNER", WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.OWNER),
        Arguments.of("OWNER", WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.READER),
        Arguments.of("WRITER", WorkspaceAccessLevel.WRITER, WorkspaceAccessLevel.READER),
        Arguments.of("READER", WorkspaceAccessLevel.READER, WorkspaceAccessLevel.READER),
        Arguments.of("PROJECT_OWNER", WorkspaceAccessLevel.OWNER, WorkspaceAccessLevel.WRITER));
  }

  private static Stream<Arguments> enforcedAccessLevels_invalid() {
    return Stream.of(
        Arguments.of("WRITER", WorkspaceAccessLevel.OWNER, ForbiddenException.class),
        Arguments.of("READER", WorkspaceAccessLevel.WRITER, ForbiddenException.class),
        Arguments.of("NO ACCESS", WorkspaceAccessLevel.READER, ForbiddenException.class),
        Arguments.of("NO ACCESS", WorkspaceAccessLevel.OWNER, ForbiddenException.class),
        Arguments.of("invalid status", WorkspaceAccessLevel.READER, ServerErrorException.class));
  }

  @ParameterizedTest(name = "enforceWorkspaceAccessLevel({0} user access, {2} required)")
  @MethodSource("enforcedAccessLevels_valid")
  public void test_enforceWorkspaceAccessLevel_valid(
      String accessLevel, WorkspaceAccessLevel expected, WorkspaceAccessLevel required) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubFcGetWorkspace(namespace, fcName, accessLevel);
    assertThat(workspaceAuthService.enforceWorkspaceAccessLevel(namespace, fcName, required))
        .isEqualTo(expected);
  }

  @ParameterizedTest(
      name = "enforceWorkspaceAccessLevel({0} user access, {1} required, expected exception {2})")
  @MethodSource("enforcedAccessLevels_invalid")
  public void test_enforceWorkspaceAccessLevel_invalid(
      String accessLevel,
      WorkspaceAccessLevel required,
      Class<? extends Throwable> expectedException) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubFcGetWorkspace(namespace, fcName, accessLevel);
    assertThrows(
        expectedException,
        () -> workspaceAuthService.enforceWorkspaceAccessLevel(namespace, fcName, required));
  }

  private void stubDaoGetRequired(String namespace, String fcName, BillingStatus billingStatus) {
    final DbWorkspace toReturn =
        new DbWorkspace()
            .setWorkspaceNamespace(namespace)
            .setFirecloudName(fcName)
            .setBillingStatus(billingStatus);
    when(mockWorkspaceDao.getRequired(namespace, fcName)).thenReturn(toReturn);
  }

  private void stubFcGetWorkspace(String namespace, String fcName, String accessLevel) {
    final FirecloudWorkspaceResponse toReturn =
        new FirecloudWorkspaceResponse()
            .workspace(new FirecloudWorkspaceDetails().namespace(namespace).name(fcName))
            .accessLevel(accessLevel);
    when(mockFireCloudService.getWorkspace(namespace, fcName)).thenReturn(toReturn);
  }
}
