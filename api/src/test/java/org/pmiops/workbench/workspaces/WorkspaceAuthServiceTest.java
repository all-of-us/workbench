package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class WorkspaceAuthServiceTest {

  @Autowired private AccessTierDao accessTierDao;

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @MockBean private AccessTierService mockAccessTierService;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private WorkspaceDao mockWorkspaceDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, WorkspaceAuthService.class})
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

  // Arguments are (original Workspace ACL), (ACL updates to make), (expected result Workspace ACL)
  private static Stream<Arguments> updateWorkspaceAcls() {
    return Stream.of(
        // trivial case: do nothing to empty ACL
        Arguments.of(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of()),

        // add one entry to empty ACL -> expect that one entry in response
        Arguments.of(
            ImmutableMap.of(),
            ImmutableMap.of("newuser", WorkspaceAccessLevel.OWNER),
            ImmutableList.of(
                FirecloudTransforms.buildAclUpdate("newuser", WorkspaceAccessLevel.OWNER))),

        // update 1 of an existing ACL of 2 -> expect to see that update and remove the other
        Arguments.of(
            ImmutableMap.of(
                "user1",
                    new FirecloudWorkspaceAccessEntry()
                        .accessLevel(WorkspaceAccessLevel.WRITER.toString()),
                "user2",
                    new FirecloudWorkspaceAccessEntry()
                        .accessLevel(WorkspaceAccessLevel.OWNER.toString())),
            ImmutableMap.of("user1", WorkspaceAccessLevel.READER),
            buildAclUpdates(
                ImmutableMap.of(
                    "user1",
                    WorkspaceAccessLevel.READER,
                    "user2",
                    WorkspaceAccessLevel.NO_ACCESS))));
  }

  @ParameterizedTest
  @MethodSource("updateWorkspaceAcls")
  public void test_updateWorkspaceAcls(
      Map<String, FirecloudWorkspaceAccessEntry> originalAcl,
      Map<String, WorkspaceAccessLevel> updates,
      List<FirecloudWorkspaceACLUpdate> expectedFcUpdates) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";

    stubRegisteredTier();
    DbWorkspace workspace = stubDaoGetRequired(namespace, fcName, BillingStatus.ACTIVE);
    stubFcGetAcl(namespace, fcName, originalAcl);
    stubUpdateAcl(namespace, fcName);

    workspaceAuthService.updateWorkspaceAcls(workspace, updates);
    verify(mockFireCloudService).updateWorkspaceACL(namespace, fcName, expectedFcUpdates);
  }

  private DbWorkspace stubDaoGetRequired(
      String namespace, String fcName, BillingStatus billingStatus) {
    final DbWorkspace toReturn =
        new DbWorkspace()
            .setWorkspaceNamespace(namespace)
            .setFirecloudName(fcName)
            .setBillingStatus(billingStatus);
    when(mockWorkspaceDao.getRequired(namespace, fcName)).thenReturn(toReturn);
    return toReturn;
  }

  private void stubFcGetWorkspace(String namespace, String fcName, String accessLevel) {
    final FirecloudWorkspaceResponse toReturn =
        new FirecloudWorkspaceResponse()
            .workspace(new FirecloudWorkspaceDetails().namespace(namespace).name(fcName))
            .accessLevel(accessLevel);
    when(mockFireCloudService.getWorkspace(namespace, fcName)).thenReturn(toReturn);
  }

  private void stubFcGetAcl(
      String namespace, String fcName, Map<String, FirecloudWorkspaceAccessEntry> acl) {
    final FirecloudWorkspaceACL toReturn = new FirecloudWorkspaceACL().acl(acl);
    when(mockFireCloudService.getWorkspaceAclAsService(namespace, fcName)).thenReturn(toReturn);
  }

  private void stubRegisteredTier() {
    when(mockAccessTierService.getRegisteredTierOrThrow())
        .thenReturn(TestMockFactory.createRegisteredTierForTests(accessTierDao));
  }

  private void stubUpdateAcl(String namespace, String fcName) {
    when(mockFireCloudService.updateWorkspaceACL(eq(namespace), eq(fcName), any()))
        .thenReturn(new FirecloudWorkspaceACLUpdateResponseList());
  }

  @NotNull
  private static List<FirecloudWorkspaceACLUpdate> buildAclUpdates(
      Map<String, WorkspaceAccessLevel> aclUpdates) {
    return aclUpdates.entrySet().stream()
        .map(e -> FirecloudTransforms.buildAclUpdate(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }
}
