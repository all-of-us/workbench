package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.common.collect.ImmutableMap;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class WorkspaceAuthServiceTest {
  private static DbUser currentUser;

  @Autowired private AccessTierDao accessTierDao;

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @MockBean private AccessTierService mockAccessTierService;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private WorkspaceDao mockWorkspaceDao;

  private static WorkbenchConfig config;
  private static final String namespace = "wsns";
  private static final String fcName = "firecloudname";

  @TestConfiguration
  @Import({FakeClockConfiguration.class, WorkspaceAuthService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig config() {
      return config;
    }
  }

  @BeforeEach
  public void setUp() {
    currentUser = new DbUser();
    config = WorkbenchConfig.createEmptyConfig();
  }

  @Test
  public void test_validateInitialCreditUsage_valid_initial_credits() {
    stubDaoGetRequired(true, false);
    assertDoesNotThrow(() -> workspaceAuthService.validateInitialCreditUsage(namespace, fcName));
  }

  @Test
  public void test_validateInitialCreditUsage_valid_billing_account() {
    stubDaoGetRequired(false, false);
    assertDoesNotThrow(() -> workspaceAuthService.validateInitialCreditUsage(namespace, fcName));
  }

  @Test
  public void test_validateInitialCreditUsage_exhausted() {
    stubDaoGetRequired(true, true);

    assertThrows(
        ForbiddenException.class,
        () -> workspaceAuthService.validateInitialCreditUsage(namespace, fcName));
  }

  @Test
  public void test_validateInitialCreditUsage_expired() {
    stubDaoGetRequired(true, false);
    when(mockInitialCreditsService.areUserCreditsExpired(currentUser)).thenReturn(true);

    assertThrows(
        ForbiddenException.class,
        () -> workspaceAuthService.validateInitialCreditUsage(namespace, fcName));
  }

  private static Stream<Arguments> accessLevels() {
    return Stream.of(
        Arguments.of("OWNER", WorkspaceAccessLevel.OWNER),
        Arguments.of("WRITER", WorkspaceAccessLevel.WRITER),
        Arguments.of("READER", WorkspaceAccessLevel.READER),
        Arguments.of("NO_ACCESS", WorkspaceAccessLevel.NO_ACCESS),
        Arguments.of("PROJECT_OWNER", WorkspaceAccessLevel.OWNER));
  }

  @ParameterizedTest(name = "getWorkspaceAccessLevel({0})")
  @MethodSource("accessLevels")
  public void test_getWorkspaceAccessLevel_valid(
      RawlsWorkspaceAccessLevel accessLevel, WorkspaceAccessLevel expected) {
    stubFcGetWorkspace(namespace, fcName, accessLevel);
    assertThat(workspaceAuthService.getWorkspaceAccessLevel(namespace, fcName)).isEqualTo(expected);
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
        Arguments.of("NO_ACCESS", WorkspaceAccessLevel.READER, ForbiddenException.class),
        Arguments.of("NO_ACCESS", WorkspaceAccessLevel.OWNER, ForbiddenException.class));
  }

  @ParameterizedTest(name = "enforceWorkspaceAccessLevel({0} user access, {2} required)")
  @MethodSource("enforcedAccessLevels_valid")
  public void test_enforceWorkspaceAccessLevel_valid(
      RawlsWorkspaceAccessLevel accessLevel,
      WorkspaceAccessLevel expected,
      WorkspaceAccessLevel required) {
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
      RawlsWorkspaceAccessLevel accessLevel,
      WorkspaceAccessLevel required,
      Class<? extends Throwable> expectedException) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubFcGetWorkspace(namespace, fcName, accessLevel);
    assertThrows(
        expectedException,
        () -> workspaceAuthService.enforceWorkspaceAccessLevel(namespace, fcName, required));
  }

  // Arguments are (original Workspace ACL), (ACL updates to make), (expected result Workspace ACL),
  // (expected remove BP from owner count), (expected add BP to owner count)
  private static Stream<Arguments> patchWorkspaceAcl() {
    return Stream.of(
        // trivial case: do nothing to empty ACL
        Arguments.of(ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(), 0, 0),

        // add one entry to empty ACL -> expect that one entry in response
        Arguments.of(
            ImmutableMap.of(),
            ImmutableMap.of("newuser", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of("newuser", WorkspaceAccessLevel.OWNER),
            0,
            1), // add newuser

        // do nothing to existing ACL -> expect no updates
        Arguments.of(
            ImmutableMap.of(
                "user1",
                WorkspaceAccessLevel.OWNER,
                "user2",
                WorkspaceAccessLevel.WRITER,
                "user3",
                WorkspaceAccessLevel.READER),
            ImmutableMap.of(),
            ImmutableMap.of(),
            0, // user1 should be ignored, NOT removed
            0),

        // add 1 entry to an existing ACL of 1 and explicitly include all existing -> expect all
        Arguments.of(
            ImmutableMap.of("user1", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of(
                "user1", WorkspaceAccessLevel.OWNER, "user2", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of(
                "user1", WorkspaceAccessLevel.OWNER, "user2", WorkspaceAccessLevel.OWNER),
            0,
            1),

        // update 1 of an existing ACL of 2 -> expect to see that update only
        Arguments.of(
            ImmutableMap.of(
                "user1", WorkspaceAccessLevel.OWNER,
                "user2", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of("user1", WorkspaceAccessLevel.READER),
            ImmutableMap.of("user1", WorkspaceAccessLevel.READER),
            1, // remove user1 but ignore user2
            0),

        // add 1 to an existing ACL of 1 -> expect only that addition
        Arguments.of(
            ImmutableMap.of("user1", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of("user2", WorkspaceAccessLevel.READER),
            ImmutableMap.of("user2", WorkspaceAccessLevel.READER),
            0, // user1 should be ignored, NOT removed
            0),

        // add 1 to an existing ACL of 1 and explicitly remove the existing 1
        Arguments.of(
            ImmutableMap.of("user1", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of(
                "user1", WorkspaceAccessLevel.NO_ACCESS, "user2", WorkspaceAccessLevel.OWNER),
            ImmutableMap.of(
                "user1", WorkspaceAccessLevel.NO_ACCESS, "user2", WorkspaceAccessLevel.OWNER),
            1, // user1 should be removed
            1)); // user2 should be added
  }

  @ParameterizedTest
  @MethodSource("patchWorkspaceAcl")
  public void test_patchWorkspaceAcl(
      Map<String, WorkspaceAccessLevel> originalAcl,
      Map<String, WorkspaceAccessLevel> updates,
      Map<String, WorkspaceAccessLevel> expectedFcUpdates,
      int expectedBpRemovals,
      int expectedBpAdditions) {
    final String namespace = "wsns";
    final String fcName = "firecloudname";

    stubRegisteredTier();
    stubUpdateAcl(namespace, fcName);
    stubFcGetAcl(namespace, fcName, originalAcl);
    DbWorkspace workspace = stubDaoGetRequired(true, false);

    workspaceAuthService.patchWorkspaceAcl(workspace, updates);
    verify(mockFireCloudService)
        .updateWorkspaceACL(namespace, fcName, buildAclUpdates(expectedFcUpdates));
    verify(mockFireCloudService, times(expectedBpRemovals))
        .removeOwnerFromBillingProjectAsService(anyString(), anyString());
    verify(mockFireCloudService, times(expectedBpAdditions))
        .addOwnerToBillingProject(anyString(), anyString());
  }

  @Test
  public void validateWorkspaceTierAccess_userHasControlledTierAccess() {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setCdrVersion(
        new DbCdrVersion()
            .setAccessTier(
                new DbAccessTier().setShortName(AccessTierService.CONTROLLED_TIER_SHORT_NAME)));
    when(mockAccessTierService.getAccessTierShortNamesForUser(any(DbUser.class)))
        .thenReturn(List.of(AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    assertDoesNotThrow(() -> workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace));
  }

  @Test
  public void validateWorkspaceTierAccess_userHasRegisteredTierAccess() {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setCdrVersion(
        new DbCdrVersion()
            .setAccessTier(
                new DbAccessTier().setShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)));
    when(mockAccessTierService.getAccessTierShortNamesForUser(any(DbUser.class)))
        .thenReturn(
            List.of(
                AccessTierService.REGISTERED_TIER_SHORT_NAME,
                AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    assertDoesNotThrow(() -> workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace));
  }

  @Test
  public void validateWorkspaceTierAccess_userDoesNotHaveControlledTierAccess() {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setCdrVersion(
        new DbCdrVersion()
            .setAccessTier(
                new DbAccessTier().setShortName(AccessTierService.CONTROLLED_TIER_SHORT_NAME)));
    when(mockAccessTierService.getAccessTierShortNamesForUser(any(DbUser.class)))
        .thenReturn(List.of(AccessTierService.REGISTERED_TIER_SHORT_NAME));

    ForbiddenException thrown =
        assertThrows(
            ForbiddenException.class,
            () -> workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace));

    assertThat(thrown).hasMessageThat().contains("User with username");
  }

  @Test
  public void validateWorkspaceTierAccess_userDoesNotHaveRegisteredTierAccess() {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setCdrVersion(
        new DbCdrVersion()
            .setAccessTier(
                new DbAccessTier().setShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)));
    when(mockAccessTierService.getAccessTierShortNamesForUser(any(DbUser.class)))
        .thenReturn(List.of(AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    ForbiddenException thrown =
        assertThrows(
            ForbiddenException.class,
            () -> workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace));

    assertThat(thrown).hasMessageThat().contains("User with username");
  }

  private DbWorkspace stubDaoGetRequired(boolean initialCredits, boolean initialCreditsExhausted) {
    final DbUser user = new DbUser();
    final DbWorkspace toReturn =
        new DbWorkspace()
            .setWorkspaceNamespace(namespace)
            .setFirecloudName(fcName)
            .setInitialCreditsExhausted(initialCreditsExhausted)
            .setBillingAccountName(
                initialCredits
                    ? config.billing.initialCreditsBillingAccountName()
                    : "personal-billing-account")
            .setCreator(user);
    when(mockWorkspaceDao.getRequired(namespace, fcName)).thenReturn(toReturn);
    return toReturn;
  }

  private void stubFcGetWorkspace(
      String namespace, String fcName, RawlsWorkspaceAccessLevel accessLevel) {
    final RawlsWorkspaceResponse toReturn =
        new RawlsWorkspaceResponse()
            .workspace(new RawlsWorkspaceDetails().namespace(namespace).name(fcName))
            .accessLevel(accessLevel);
    when(mockFireCloudService.getWorkspace(namespace, fcName)).thenReturn(toReturn);
  }

  private void stubFcGetAcl(
      String namespace, String fcName, Map<String, WorkspaceAccessLevel> acl) {
    when(mockFireCloudService.getWorkspaceAclAsService(namespace, fcName))
        .thenReturn(
            new RawlsWorkspaceACL()
                .acl(
                    acl.entrySet().stream()
                        .collect(
                            Collectors.toMap(
                                Entry::getKey,
                                e ->
                                    new RawlsWorkspaceAccessEntry()
                                        .accessLevel(e.getValue().toString())))));
  }

  private void stubRegisteredTier() {
    when(mockAccessTierService.getRegisteredTierOrThrow())
        .thenReturn(accessTierDao.save(createRegisteredTier()));
  }

  private void stubUpdateAcl(String namespace, String fcName) {
    when(mockFireCloudService.updateWorkspaceACL(eq(namespace), eq(fcName), any()))
        .thenReturn(new RawlsWorkspaceACLUpdateResponseList());
  }

  @NotNull
  private static List<RawlsWorkspaceACLUpdate> buildAclUpdates(
      Map<String, WorkspaceAccessLevel> aclUpdates) {
    return aclUpdates.entrySet().stream()
        .map(e -> FirecloudTransforms.buildAclUpdate(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }
}
