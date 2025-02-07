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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
