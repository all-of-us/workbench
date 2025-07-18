package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;
import static org.springframework.test.util.AssertionErrors.fail;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
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
  public void findAllActiveWorkspaceNamespaces_empty() {
    workspaceDao.deleteAll();

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    assertThat(namespaces).isEmpty();
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_singleActive() {
    workspaceDao.deleteAll();

    String namespace = "test-namespace-1";
    DbWorkspace workspace =
        createWorkspace()
            .setWorkspaceNamespace(namespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspaceDao.save(workspace);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    assertThat(namespaces).containsExactly(namespace);
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_multipleActive() {
    workspaceDao.deleteAll();

    String namespace1 = "test-namespace-1";
    String namespace2 = "test-namespace-2";
    String namespace3 = "test-namespace-3";

    DbWorkspace workspace1 =
        createWorkspace()
            .setWorkspaceNamespace(namespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace workspace2 =
        createWorkspace()
            .setWorkspaceNamespace(namespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace workspace3 =
        createWorkspace()
            .setWorkspaceNamespace(namespace3)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(workspace1);
    workspaceDao.save(workspace2);
    workspaceDao.save(workspace3);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    assertThat(namespaces).containsExactly(namespace1, namespace2, namespace3);
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_excludesDeleted() {
    workspaceDao.deleteAll();

    String activeNamespace = "active-namespace";
    String deletedNamespace = "deleted-namespace";

    DbWorkspace activeWorkspace =
        createWorkspace()
            .setWorkspaceNamespace(activeNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace deletedWorkspace =
        createWorkspace()
            .setWorkspaceNamespace(deletedNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);

    workspaceDao.save(activeWorkspace);
    workspaceDao.save(deletedWorkspace);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    assertThat(namespaces).containsExactly(activeNamespace);
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_duplicateNamespaces() {
    workspaceDao.deleteAll();

    String sharedNamespace = "shared-namespace";

    // Create two workspaces with the same namespace but different firecloud names
    DbWorkspace workspace1 =
        createWorkspace()
            .setWorkspaceNamespace(sharedNamespace)
            .setFirecloudName("firecloud-name-1")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace workspace2 =
        createWorkspace()
            .setWorkspaceNamespace(sharedNamespace)
            .setFirecloudName("firecloud-name-2")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(workspace1);
    workspaceDao.save(workspace2);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    // Should only return one instance of the shared namespace due to DISTINCT
    assertThat(namespaces).containsExactly(sharedNamespace);
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_mixedStatusesWithDuplicates() {
    workspaceDao.deleteAll();

    String activeNamespace1 = "active-namespace-1";
    String activeNamespace2 = "active-namespace-2";
    String deletedNamespace = "deleted-namespace";
    String sharedNamespace = "shared-namespace";

    // Active workspaces
    DbWorkspace activeWorkspace1 =
        createWorkspace()
            .setWorkspaceNamespace(activeNamespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace activeWorkspace2 =
        createWorkspace()
            .setWorkspaceNamespace(activeNamespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    // Deleted workspace
    DbWorkspace deletedWorkspace =
        createWorkspace()
            .setWorkspaceNamespace(deletedNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);

    // Multiple active workspaces with shared namespace
    DbWorkspace sharedActiveWorkspace1 =
        createWorkspace()
            .setWorkspaceNamespace(sharedNamespace)
            .setFirecloudName("shared-firecloud-1")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace sharedActiveWorkspace2 =
        createWorkspace()
            .setWorkspaceNamespace(sharedNamespace)
            .setFirecloudName("shared-firecloud-2")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    // One deleted workspace with shared namespace
    DbWorkspace sharedDeletedWorkspace =
        createWorkspace()
            .setWorkspaceNamespace(sharedNamespace)
            .setFirecloudName("shared-firecloud-deleted")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);

    workspaceDao.save(activeWorkspace1);
    workspaceDao.save(activeWorkspace2);
    workspaceDao.save(deletedWorkspace);
    workspaceDao.save(sharedActiveWorkspace1);
    workspaceDao.save(sharedActiveWorkspace2);
    workspaceDao.save(sharedDeletedWorkspace);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    // Should return distinct namespaces from active workspaces only
    assertThat(namespaces).containsExactly(activeNamespace1, activeNamespace2, sharedNamespace);
  }

  @Test
  public void findAllActiveWorkspaceNamespaces_onlyDeletedWorkspaces() {
    workspaceDao.deleteAll();

    String deletedNamespace1 = "deleted-namespace-1";
    String deletedNamespace2 = "deleted-namespace-2";

    DbWorkspace deletedWorkspace1 =
        createWorkspace()
            .setWorkspaceNamespace(deletedNamespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    DbWorkspace deletedWorkspace2 =
        createWorkspace()
            .setWorkspaceNamespace(deletedNamespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);

    workspaceDao.save(deletedWorkspace1);
    workspaceDao.save(deletedWorkspace2);

    List<String> namespaces = workspaceDao.findAllActiveWorkspaceNamespaces();

    assertThat(namespaces).isEmpty();
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_empty() {
    workspaceDao.deleteAll();

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(Collections.emptyList());

    assertThat(orphanedNamespaces).isEmpty();
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_noReferencedWorkspaces() {
    workspaceDao.deleteAll();

    String namespace1 = "orphaned-namespace-1";
    String namespace2 = "orphaned-namespace-2";

    DbWorkspace workspace1 = 
        createWorkspace()
            .setWorkspaceNamespace(namespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace workspace2 = 
        createWorkspace()
            .setWorkspaceNamespace(namespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(workspace1);
    workspaceDao.save(workspace2);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(Collections.emptyList());

    assertThat(orphanedNamespaces).containsExactly(namespace1, namespace2);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_singleOrphaned() {
    workspaceDao.deleteAll();

    String orphanedNamespace = "orphaned-namespace";
    String knownNamespace = "known-namespace";

    DbWorkspace orphanedWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace knownWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(knownNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(orphanedWorkspace);
    workspaceDao.save(knownWorkspace);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(List.of(knownNamespace));

    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_multipleOrphaned() {
    workspaceDao.deleteAll();

    String orphanedNamespace1 = "orphaned-namespace-1";
    String orphanedNamespace2 = "orphaned-namespace-2";
    String knownNamespace1 = "known-namespace-1";
    String knownNamespace2 = "known-namespace-2";

    DbWorkspace orphanedWorkspace1 = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace orphanedWorkspace2 = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace knownWorkspace1 = 
        createWorkspace()
            .setWorkspaceNamespace(knownNamespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace knownWorkspace2 = 
        createWorkspace()
            .setWorkspaceNamespace(knownNamespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(orphanedWorkspace1);
    workspaceDao.save(orphanedWorkspace2);
    workspaceDao.save(knownWorkspace1);
    workspaceDao.save(knownWorkspace2);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(
        List.of(knownNamespace1, knownNamespace2));

    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace1, orphanedNamespace2);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_noOrphans() {
    workspaceDao.deleteAll();

    String namespace1 = "namespace-1";
    String namespace2 = "namespace-2";

    DbWorkspace workspace1 = 
        createWorkspace()
            .setWorkspaceNamespace(namespace1)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace workspace2 = 
        createWorkspace()
            .setWorkspaceNamespace(namespace2)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(workspace1);
    workspaceDao.save(workspace2);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(
        List.of(namespace1, namespace2));

    assertThat(orphanedNamespaces).isEmpty();
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_excludesDeletedWorkspaces() {
    workspaceDao.deleteAll();

    String deletedNamespace = "deleted-namespace";
    String orphanedNamespace = "orphaned-namespace";
    String knownNamespace = "known-namespace";

    DbWorkspace deletedWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(deletedNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    DbWorkspace orphanedWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace knownWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(knownNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(deletedWorkspace);
    workspaceDao.save(orphanedWorkspace);
    workspaceDao.save(knownWorkspace);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(List.of(knownNamespace));

    // Should only return the active orphaned workspace, not the deleted one
    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace);
  }

  @Test
  public void findAllOrphanedWorkspaceNamespaces_duplicateNamespaces() {
    workspaceDao.deleteAll();

    String orphanedNamespace = "orphaned-namespace";
    String knownNamespace = "known-namespace";

    // Create multiple workspaces with the same orphaned namespace
    DbWorkspace orphanedWorkspace1 = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace)
            .setFirecloudName("firecloud-1")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace orphanedWorkspace2 = 
        createWorkspace()
            .setWorkspaceNamespace(orphanedNamespace)
            .setFirecloudName("firecloud-2")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    DbWorkspace knownWorkspace = 
        createWorkspace()
            .setWorkspaceNamespace(knownNamespace)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(orphanedWorkspace1);
    workspaceDao.save(orphanedWorkspace2);
    workspaceDao.save(knownWorkspace);

    List<String> orphanedNamespaces = workspaceDao.findAllOrphanedWorkspaceNamespaces(List.of(knownNamespace));

    // Should only return one instance of the orphaned namespace due to DISTINCT
    assertThat(orphanedNamespaces).containsExactly(orphanedNamespace);
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
