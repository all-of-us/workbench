package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
public class WorkspaceAuthServiceTest {
  private static DbUser currentUser;

  @Autowired private AccessTierDao accessTierDao;

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @MockitoBean private AccessTierService mockAccessTierService;
  @MockitoBean private InitialCreditsService mockInitialCreditsService;
  @MockitoBean private WorkspaceDao mockWorkspaceDao;

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
