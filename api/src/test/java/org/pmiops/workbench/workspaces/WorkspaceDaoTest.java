package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;
import static org.springframework.test.util.AssertionErrors.fail;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceUserCacheDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceDaoTest {
  private final long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
  private static final String WORKSPACE_1_NAME = "Foo";
  private static final String WORKSPACE_NAMESPACE = "aou-1";
  private static final String GOOGLE_PROJECT = "gcp-proj-1";

  private DbUser dbUser;
  private DbWorkspace dbWorkspace;
  private DbInstitution dbInstitution;

  @Autowired AccessTierDao accessTierDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired InstitutionDao institutionDao;
  @Autowired UserDao userDao;
  @Autowired ReportingTestFixture<DbUser, ReportingUser> userFixture;
  @Autowired VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceUserCacheDao workspaceUserCacheDao;

  @TestConfiguration
  @Import({CommonMappers.class, ReportingTestConfig.class})
  @MockBean({Clock.class})
  public static class config {}

  @BeforeEach
  public void setUp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    dbWorkspace = new DbWorkspace();
    dbWorkspace.setName("name");
    dbWorkspace.setWorkspaceNamespace("name");
    dbWorkspace.setFirecloudName("name");
    dbWorkspace.setCreationTime(timestamp);
    dbWorkspace.setLastModifiedTime(timestamp);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    dbUser = userDao.save(new DbUser());

    dbInstitution =
        new DbInstitution()
            .setShortName("Test Institution")
            .setDisplayName("Test Institution")
            .setBypassInitialCreditsExpiration(false);
    dbInstitution = institutionDao.save(dbInstitution);
    verifiedInstitutionalAffiliationDao.save(
        new DbVerifiedInstitutionalAffiliation()
            .setInstitution(dbInstitution)
            .setUser(dbUser)
            .setInstitutionalRoleEnum(InstitutionalRole.HIGH_SCHOOL_STUDENT));
  }

  @Test
  public void testWorkspaceVersionLocking() {
    workspaceDao.deleteAll();

    DbWorkspace ws = new DbWorkspace();
    ws.setVersion(1);
    ws = workspaceDao.save(ws);

    // Version incremented to 2.
    ws.setName("foo");
    ws = workspaceDao.save(ws);

    try {
      ws.setName("bar");
      ws.setVersion(1);
      workspaceDao.save(ws);
      fail("expected optimistic lock exception on stale version update");
    } catch (ObjectOptimisticLockingFailureException e) {
      // expected
    }
  }

  @Test
  public void testGetWorkspaceByGoogleProject() {
    workspaceDao.deleteAll();

    DbWorkspace dbWorkspace = createWorkspace();
    assertThat(workspaceDao.getByGoogleProject(GOOGLE_PROJECT).get().getName())
        .isEqualTo(dbWorkspace.getName());
    assertThat(workspaceDao.getByGoogleProject(GOOGLE_PROJECT).get().getGoogleProject())
        .isEqualTo(dbWorkspace.getGoogleProject());
  }

  @Test
  public void testAdminLocked() {
    workspaceDao.deleteAll();

    DbWorkspace ws = new DbWorkspace();
    assertThat(ws.isAdminLocked()).isFalse();
    assertThat(ws.getAdminLockedReason()).isNull();

    String reason = "just because";
    ws.setAdminLocked(true);
    ws.setAdminLockedReason(reason);
    ws = workspaceDao.save(ws);
    assertThat(ws.isAdminLocked()).isTrue();
    assertThat(ws.getAdminLockedReason()).isEqualTo(reason);
  }

  @Test
  public void findCreatorsByActiveInitialCredits_notCreator() {
    DbUser differentUser = userDao.save(userFixture.createEntity());
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(differentUser);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_differentBillingAccount() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("personalAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_missingInitialCreditsRecord() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withUnexpiredCredits() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);

    dbUser.setUserInitialCreditsExpiration(
        new DbUserInitialCreditsExpiration()
            .setExpirationTime(new Timestamp(System.currentTimeMillis() + MILLIS_IN_A_DAY)));
    userDao.save(dbUser);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withExpiredCredits() {
    DbUserInitialCreditsExpiration dbUserInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setExpirationTime(new Timestamp(System.currentTimeMillis() - MILLIS_IN_A_DAY))
            .setUser(dbUser);
    dbUser = dbUser.setUserInitialCreditsExpiration(dbUserInitialCreditsExpiration);
    dbUser = userDao.save(dbUser);
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withExpiredCreditsButIndividuallyBypassed() {
    DbUserInitialCreditsExpiration dbUserInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setExpirationTime(new Timestamp(System.currentTimeMillis() - MILLIS_IN_A_DAY))
            .setBypassed(true)
            .setUser(dbUser);
    dbUser = dbUser.setUserInitialCreditsExpiration(dbUserInitialCreditsExpiration);
    dbUser = userDao.save(dbUser);
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withExpiredCreditsButInstitutionallyBypassed() {
    DbUserInitialCreditsExpiration dbUserInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setExpirationTime(new Timestamp(System.currentTimeMillis() - MILLIS_IN_A_DAY))
            .setUser(dbUser);
    dbUser = dbUser.setUserInitialCreditsExpiration(dbUserInitialCreditsExpiration);
    dbUser = userDao.save(dbUser);
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    institutionDao.save(dbInstitution.setBypassInitialCreditsExpiration(true));

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_initialCreditsExhausted() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace.setInitialCreditsExhausted(true);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findNamespacesByActiveStatusAndFirecloudUuidsIn_none() {
    assertThat(workspaceDao.findNamespacesByActiveStatusAndFirecloudUuidIn(Collections.emptyList()))
        .isEmpty();
  }

  @Test
  public void findNamespacesByActiveStatusAndFirecloudUuidsIn_one() {
    String namespace = "my-namespace";
    String uuid = "my-uuid";

    workspaceDao.save(
        dbWorkspace
            .setWorkspaceNamespace(namespace)
            .setFirecloudUuid(uuid)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    assertThat(
            workspaceDao.findNamespacesByActiveStatusAndFirecloudUuidIn(
                Collections.singleton(uuid)))
        .containsExactly(namespace);
  }

  @Test
  public void findNamespacesByActiveStatusAndFirecloudUuidsIn_multiple() {
    DbWorkspace ws1 =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceNamespace("something")
                .setFirecloudUuid("123")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    DbWorkspace ws2 =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceNamespace("something-else")
                .setFirecloudUuid("456")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    // these two won't match

    DbWorkspace deleted =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceNamespace("gone")
                .setFirecloudUuid("bye")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED));

    workspaceDao.save(
        new DbWorkspace()
            .setWorkspaceNamespace("don't want it")
            .setFirecloudUuid("not searching for this one")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    List<String> requestedUuids =
        List.of(ws1.getFirecloudUuid(), ws2.getFirecloudUuid(), deleted.getFirecloudUuid());
    List<String> expectedNamespaces =
        List.of(ws1.getWorkspaceNamespace(), ws2.getWorkspaceNamespace());

    assertThat(workspaceDao.findNamespacesByActiveStatusAndFirecloudUuidIn(requestedUuids))
        .containsExactlyElementsIn(expectedNamespaces);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_empty() {
    workspaceDao.deleteAll();

    List<String> orphanedNamespaces =
        workspaceDao.findAllOrphanedWorkspaceNamespaces(Collections.emptyList());

    assertThat(orphanedNamespaces).isEmpty();
  }

  // Helper method to create and save a workspace with specific namespace and status
  private void createAndSaveWorkspace(String namespace, WorkspaceActiveStatus status) {
    workspaceDao.save(
        createWorkspace().setWorkspaceNamespace(namespace).setWorkspaceActiveStatusEnum(status));
  }

  // Helper method to create and save a workspace with specific namespace, status, and firecloud
  // name
  private void createAndSaveWorkspace(
      String namespace, WorkspaceActiveStatus status, String firecloudName) {
    workspaceDao.save(
        createWorkspace()
            .setWorkspaceNamespace(namespace)
            .setFirecloudName(firecloudName)
            .setWorkspaceActiveStatusEnum(status));
  }

  // Test data for orphaned workspace scenarios
  static Stream<Arguments> orphanedWorkspaceScenarios() {
    return Stream.of(
        Arguments.of(
            "No referenced workspaces - all orphaned",
            List.of("orphaned-namespace-1", "orphaned-namespace-2"), // namespaces in db
            Collections.emptyList(), // externally referenced namespaces
            List.of("orphaned-namespace-1", "orphaned-namespace-2") // expected orphaned
            ),
        Arguments.of(
            "Single orphaned workspace",
            List.of("orphaned-namespace"), // namespaces in db
            List.of("externally-referenced-namespace"), // externally referenced namespaces
            List.of("orphaned-namespace") // expected orphaned
            ),
        Arguments.of(
            "Multiple orphaned workspaces",
            List.of("orphaned-namespace-1", "orphaned-namespace-2"), // namespaces in db
            List.of(
                "externally-referenced-namespace-1",
                "externally-referenced-namespace-2"), // externally referenced namespaces
            List.of("orphaned-namespace-1", "orphaned-namespace-2") // expected orphaned
            ),
        Arguments.of(
            "No orphaned workspaces",
            List.of("namespace-1", "namespace-2"), // namespaces in db
            List.of("namespace-1", "namespace-2"), // externally referenced namespaces
            Collections.emptyList() // expected orphaned
            ),
        Arguments.of(
            "Mixed bag - some orphaned, some externally referenced",
            List.of("orphaned-1", "orphaned-2", "referenced-1", "referenced-2"), // namespaces in db
            List.of("referenced-1", "referenced-2"), // externally referenced namespaces
            List.of("orphaned-1", "orphaned-2") // expected orphaned
            ));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("orphanedWorkspaceScenarios")
  void testFindAllOrphanedWorkspaceNamespaces_basicScenarios(
      String scenarioName,
      List<String> workspaceNamespaces,
      List<String> externallyReferencedNamespaces,
      List<String> expectedOrphaned) {
    workspaceDao.deleteAll();

    // Create and save all workspaces
    for (String namespace : workspaceNamespaces) {
      createAndSaveWorkspace(namespace, WorkspaceActiveStatus.ACTIVE);
    }

    // Execute the query
    List<String> orphanedNamespaces =
        workspaceDao.findAllOrphanedWorkspaceNamespaces(externallyReferencedNamespaces);

    // Assert the results
    if (expectedOrphaned.isEmpty()) {
      assertThat(orphanedNamespaces).isEmpty();
    } else {
      assertThat(orphanedNamespaces).containsExactlyElementsIn(expectedOrphaned);
    }
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_excludesDeletedWorkspaces() {
    workspaceDao.deleteAll();

    String deletedNamespace = "deleted-namespace";
    String orphanedNamespace = "orphaned-namespace";
    String externallyReferencedNamespace = "known-namespace";

    createAndSaveWorkspace(deletedNamespace, WorkspaceActiveStatus.DELETED);
    createAndSaveWorkspace(orphanedNamespace, WorkspaceActiveStatus.ACTIVE);
    createAndSaveWorkspace(externallyReferencedNamespace, WorkspaceActiveStatus.ACTIVE);

    List<String> orphanedNamespaces =
        workspaceDao.findAllOrphanedWorkspaceNamespaces(List.of(externallyReferencedNamespace));

    // Should only return the active orphaned workspace, not the deleted one
    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_duplicateNamespaces() {
    workspaceDao.deleteAll();

    String orphanedNamespace = "orphaned-namespace";
    String externallyReferencedNamespace = "externally-referenced-namespace";

    // Create multiple workspaces with the same orphaned namespace
    createAndSaveWorkspace(orphanedNamespace, WorkspaceActiveStatus.ACTIVE, "firecloud-1");
    createAndSaveWorkspace(orphanedNamespace, WorkspaceActiveStatus.ACTIVE, "firecloud-2");
    createAndSaveWorkspace(externallyReferencedNamespace, WorkspaceActiveStatus.ACTIVE);

    List<String> orphanedNamespaces =
        workspaceDao.findAllOrphanedWorkspaceNamespaces(List.of(externallyReferencedNamespace));

    // Should only return one instance of the orphaned namespace due to DISTINCT
    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace);
  }

  @Test
  public void findAllActiveWorkspacesNeedingCacheUpdate_noCache() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    // Create an active workspace with no cache entries
    DbWorkspace activeWorkspace =
        new DbWorkspace()
            .setName("Active Workspace")
            .setWorkspaceNamespace("active-ws")
            .setFirecloudName("active-ws")
            .setCreationTime(new Timestamp(System.currentTimeMillis()))
            .setLastModifiedTime(new Timestamp(System.currentTimeMillis()))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    activeWorkspace = workspaceDao.save(activeWorkspace);

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();
    assertThat(result.size()).isEqualTo(1);
    var resultWorkspace = result.get(0);
    assertThat(resultWorkspace.getWorkspaceId()).isEqualTo(activeWorkspace.getWorkspaceId());
    assertThat(resultWorkspace.getWorkspaceNamespace())
        .isEqualTo(activeWorkspace.getWorkspaceNamespace());
    assertThat(resultWorkspace.getFirecloudName()).isEqualTo(activeWorkspace.getFirecloudName());
  }

  @Test
  public void findAllActiveWorkspacesNeedingCacheUpdate_staleCache() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    long baseTime = System.currentTimeMillis();

    // Create workspace
    DbWorkspace workspace =
        new DbWorkspace()
            .setWorkspaceId(1L)
            .setName("Workspace")
            .setWorkspaceNamespace("ws-namespace")
            .setFirecloudName("ws-firecloud")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime + 1000)) // Modified after cache
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspace = workspaceDao.save(workspace);

    // Create cache entry that's older than workspace modification
    DbWorkspaceUserCache cacheEntry =
        new DbWorkspaceUserCache(
            workspace.getWorkspaceId(),
            dbUser.getUserId(),
            "OWNER",
            new Timestamp(baseTime)); // Cache updated before workspace modification
    workspaceUserCacheDao.save(cacheEntry);

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();
    assertThat(result.size()).isEqualTo(1);
    var resultWorkspace = result.get(0);
    assertThat(resultWorkspace.getWorkspaceId()).isEqualTo(workspace.getWorkspaceId());
    assertThat(resultWorkspace.getWorkspaceNamespace())
        .isEqualTo(workspace.getWorkspaceNamespace());
    assertThat(resultWorkspace.getFirecloudName()).isEqualTo(workspace.getFirecloudName());
  }

  @Test
  public void findAllActiveWorkspacesNeedingCacheUpdate_freshCache() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    long baseTime = System.currentTimeMillis();

    // Create workspace
    DbWorkspace workspace =
        new DbWorkspace()
            .setName("Workspace")
            .setWorkspaceNamespace("ws-namespace")
            .setFirecloudName("ws-firecloud")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime)) // Modified before cache
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspace = workspaceDao.save(workspace);

    // Create cache entry that's newer than workspace modification
    DbWorkspaceUserCache cacheEntry =
        new DbWorkspaceUserCache(
            workspace.getWorkspaceId(),
            dbUser.getUserId(),
            "OWNER",
            new Timestamp(baseTime + 1000)); // Cache updated after workspace modification
    workspaceUserCacheDao.save(cacheEntry);

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();
    assertThat(result).isEmpty();
  }

  @Test
  public void findAllActiveWorkspaceNamespacesNeedingCacheUpdate_inactiveWorkspaceIgnored() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    // Create inactive workspace with no cache
    DbWorkspace inactiveWorkspace =
        new DbWorkspace()
            .setName("Inactive Workspace")
            .setWorkspaceNamespace("inactive-ws")
            .setFirecloudName("inactive-ws")
            .setCreationTime(new Timestamp(System.currentTimeMillis()))
            .setLastModifiedTime(new Timestamp(System.currentTimeMillis()))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(inactiveWorkspace);

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();
    assertThat(result).isEmpty();
  }

  @Test
  public void findAllActiveWorkspacesNeedingCacheUpdate_mixed() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    long baseTime = System.currentTimeMillis();

    // Workspace with no cache - should be included
    DbWorkspace workspaceNoCache =
        new DbWorkspace()
            .setName("No Cache Workspace")
            .setWorkspaceNamespace("no-cache-ws")
            .setFirecloudName("no-cache-ws")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspaceNoCache = workspaceDao.save(workspaceNoCache);

    // Workspace with stale cache - should be included
    DbWorkspace workspaceStaleCache =
        new DbWorkspace()
            .setName("Stale Cache Workspace")
            .setWorkspaceNamespace("stale-cache-ws")
            .setFirecloudName("stale-cache-ws")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime + 2000))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspaceStaleCache = workspaceDao.save(workspaceStaleCache);

    // Workspace with fresh cache - should NOT be included
    DbWorkspace workspaceFreshCache =
        new DbWorkspace()
            .setName("Fresh Cache Workspace")
            .setWorkspaceNamespace("fresh-cache-ws")
            .setFirecloudName("fresh-cache-ws")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspaceFreshCache = workspaceDao.save(workspaceFreshCache);

    // Create cache entries
    workspaceUserCacheDao.save(
        new DbWorkspaceUserCache(
            workspaceStaleCache.getWorkspaceId(),
            dbUser.getUserId(),
            "OWNER",
            new Timestamp(baseTime + 1000))); // Older than workspace modification

    workspaceUserCacheDao.save(
        new DbWorkspaceUserCache(
            workspaceFreshCache.getWorkspaceId(),
            dbUser.getUserId(),
            "OWNER",
            new Timestamp(baseTime + 1000))); // Newer than workspace modification

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.stream().map(WorkspaceDao.WorkspaceUserCacheView::getWorkspaceId).toList())
        .containsExactly(workspaceNoCache.getWorkspaceId(), workspaceStaleCache.getWorkspaceId());
  }

  @Test
  public void findAllActiveWorkspacesNeedingCacheUpdate_noDuplicates() {
    workspaceDao.deleteAll();
    workspaceUserCacheDao.deleteAll();

    long baseTime = System.currentTimeMillis();

    // Create a workspace with stale cache
    DbWorkspace workspace =
        new DbWorkspace()
            .setName("Workspace with Multiple Cache Entries")
            .setWorkspaceNamespace("multi-cache-ws")
            .setFirecloudName("multi-cache-ws")
            .setCreationTime(new Timestamp(baseTime))
            .setLastModifiedTime(new Timestamp(baseTime + 2000))
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspace = workspaceDao.save(workspace);

    DbUser user2 = userDao.save(new DbUser());

    // Create multiple stale cache entries for the same workspace (different users)
    workspaceUserCacheDao.save(
        new DbWorkspaceUserCache(
            workspace.getWorkspaceId(),
            dbUser.getUserId(),
            "OWNER",
            new Timestamp(baseTime + 1000)));

    workspaceUserCacheDao.save(
        new DbWorkspaceUserCache(
            workspace.getWorkspaceId(),
            user2.getUserId(),
            "WRITER",
            new Timestamp(baseTime + 1000)));

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceDao.findAllActiveWorkspacesNeedingCacheUpdate();

    // Should only return the workspace once, despite multiple cache entries
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getWorkspaceId()).isEqualTo(workspace.getWorkspaceId());
  }

  private DbWorkspace createWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setVersion(1);
    workspace.setName(WORKSPACE_1_NAME);
    workspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace.setGoogleProject(GOOGLE_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbCdrVersion dbCdrVersion = createDefaultCdrVersion();
    accessTierDao.save(dbCdrVersion.getAccessTier());
    dbCdrVersion = cdrVersionDao.save(dbCdrVersion);
    workspace.setCdrVersion(dbCdrVersion);
    workspace = workspaceDao.save(workspace);
    return workspace;
  }
}
