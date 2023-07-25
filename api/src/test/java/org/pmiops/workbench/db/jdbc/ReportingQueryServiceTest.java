package org.pmiops.workbench.db.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__COMPLIANCE_TRAINING_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__COMPLIANCE_TRAINING_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__ERA_COMMONS_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__ERA_COMMONS_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__IDENTITY_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__IDENTITY_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__INSTITUTIONAL_ROLE_ENUM;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__INSTITUTIONAL_ROLE_OTHER_TEXT;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__INSTITUTION_ID;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__RAS_LOGIN_GOV_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__RAS_LOGIN_GOV_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__TWO_FACTOR_AUTH_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__TWO_FACTOR_AUTH_COMPLETION_TIME;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionTierRequirementDao;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.NewUserSatisfactionSurveySatisfaction;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test the unique ReportingNativeQueryService, which bypasses Spring in favor of low-level JDBC
 * queries. This means we need real DAOs.
 */
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingQueryServiceTest {

  public static final int BATCH_SIZE = 2;
  @Autowired private ReportingQueryService reportingQueryService;

  // It's necessary to bring in several Dao classes, since we aim to populate join tables
  // that have neither entities of their own nor stand-alone DAOs.
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private InstitutionDao institutionDao;
  @Autowired InstitutionTierRequirementDao institutionTierRequirementDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired private NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;

  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;

  @Import({
    FakeClockConfiguration.class,
    ReportingQueryServiceImpl.class,
    ReportingUserFixture.class,
    ReportingTestConfig.class
  })
  @TestConfiguration
  public static class config {}

  private DbInstitution dbInstitution;
  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  private DbAccessModule twoFactorAuthModule;
  private DbAccessModule rtTrainingModule;
  private DbAccessModule eRACommonsModule;
  private DbAccessModule identityModule;
  private DbAccessModule rasLoginGovModule;
  private DbAccessModule duccModule;

  @BeforeEach
  public void setup() {
    registeredTier = accessTierDao.save(createRegisteredTier());
    controlledTier = accessTierDao.save(createControlledTier());
    TestMockFactory.createAccessModules(accessModuleDao);
    dbInstitution = createDbInstitution();
    twoFactorAuthModule = accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    rtTrainingModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();
    eRACommonsModule = accessModuleDao.findOneByName(DbAccessModuleName.ERA_COMMONS).get();
    identityModule = accessModuleDao.findOneByName(DbAccessModuleName.IDENTITY).get();
    rasLoginGovModule = accessModuleDao.findOneByName(DbAccessModuleName.RAS_LOGIN_GOV).get();
    duccModule = accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
  }

  @Test
  public void testGetReportingDatasetCohorts() {
    final DbUser user1 = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion1 = createCdrVersion(registeredTier);
    final DbWorkspace workspace1 = createDbWorkspace(user1, cdrVersion1);
    final DbCohort cohort1 = createCohort(user1, workspace1);
    final DbDataset dataset1 = createDataset(workspace1, cohort1);
    entityManager.flush();

    final List<ReportingDatasetCohort> datasetCohorts = reportingQueryService.getDatasetCohorts();
    assertThat(datasetCohorts).hasSize(1);
    assertThat(datasetCohorts.get(0).getCohortId()).isEqualTo(cohort1.getCohortId());
    assertThat(datasetCohorts.get(0).getDatasetId()).isEqualTo(dataset1.getDataSetId());
  }

  @NotNull
  @Transactional
  public DbDataset createDataset(DbWorkspace workspace1, DbCohort cohort1) {
    DbDataset dataset1 = ReportingTestUtils.createDbDataset(workspace1.getWorkspaceId());
    dataset1.setCohortIds(ImmutableList.of(cohort1.getCohortId()));
    dataset1 = dataSetDao.save(dataset1);
    assertThat(dataSetDao.count()).isEqualTo(1);
    assertThat(dataset1.getCohortIds()).containsExactly(cohort1.getCohortId());
    cohortDao.save(cohort1);
    return dataset1;
  }

  @Transactional
  public DbCohort createCohort(DbUser user1, DbWorkspace workspace1) {
    final DbCohort cohort = cohortDao.save(ReportingTestUtils.createDbCohort(user1, workspace1));
    return cohort;
  }

  @Transactional
  public DbWorkspace createDbWorkspace(DbUser user1, DbCdrVersion cdrVersion1) {
    final long initialWorkspaceCount = workspaceDao.count();
    final DbWorkspace workspace1 =
        workspaceDao.save(
            ReportingTestUtils.createDbWorkspace(user1, cdrVersion1)); // save cdr version too
    assertThat(workspaceDao.count()).isEqualTo(initialWorkspaceCount + 1);
    return workspace1;
  }

  @Transactional
  public DbCdrVersion createCdrVersion(DbAccessTier accessTier) {
    DbCdrVersion cdrVersion1 = new DbCdrVersion();
    cdrVersion1.setName("foo");
    cdrVersion1.setAccessTier(accessTier);
    cdrVersion1 = cdrVersionDao.save(cdrVersion1);
    assertThat(cdrVersionDao.count()).isEqualTo(1);
    return cdrVersion1;
  }

  @Transactional
  public DbUser createDbUserWithInstitute() {
    int currentSize = userDao.findUsers().size();
    final DbUser user = userDao.save(userFixture.createEntity());
    assertThat(userDao.count()).isEqualTo(currentSize + 1);
    createDbVerifiedInstitutionalAffiliation(user);
    return user;
  }

  @Transactional
  public DbVerifiedInstitutionalAffiliation createDbVerifiedInstitutionalAffiliation(
      DbUser dbUser) {
    DbVerifiedInstitutionalAffiliation dbVerifiedInstitutionalAffiliation =
        new DbVerifiedInstitutionalAffiliation();
    dbVerifiedInstitutionalAffiliation.setVerifiedInstitutionalAffiliationId(USER__INSTITUTION_ID);
    dbVerifiedInstitutionalAffiliation.setInstitutionalRoleEnum(USER__INSTITUTIONAL_ROLE_ENUM);
    dbVerifiedInstitutionalAffiliation.setInstitution(dbInstitution);
    dbVerifiedInstitutionalAffiliation.setInstitutionalRoleOtherText(
        USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
    dbVerifiedInstitutionalAffiliation.setUser(dbUser);
    verifiedInstitutionalAffiliationDao.save(dbVerifiedInstitutionalAffiliation);
    return dbVerifiedInstitutionalAffiliation;
  }

  @Transactional
  public DbInstitution createDbInstitution() {
    DbInstitution dbInstitution = new DbInstitution();
    dbInstitution.setShortName("dbInstitutionName");
    dbInstitution.setDisplayName("dbInstitutionName");
    institutionDao.save(dbInstitution);
    return dbInstitution;
  }

  @Test
  public void testWorkspaceIterator_oneEntry() {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    final DbWorkspace workspace = createDbWorkspace(user, cdrVersion);

    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    List<ReportingWorkspace> firstBatch = iterator.next();
    assertThat(firstBatch).hasSize(1);
    assertThat(firstBatch.get(0).getName()).isEqualTo(workspace.getName());
    assertThat(iterator.hasNext()).isFalse();
  }

  @Transactional
  public DbUserAccessTier addUserToTier(DbUser user, DbAccessTier tier) {
    return userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(tier)
            .setTierAccessStatus(TierAccessStatus.ENABLED)
            .setFirstEnabled(Timestamp.from(Instant.now()))
            .setLastUpdated(Timestamp.from(Instant.now())));
  }

  @Transactional
  public DbUserAccessModule addUserAccessModule(
      DbUser user, DbAccessModule accessModule, Timestamp bypassTime, Timestamp completionTime) {
    return userAccessModuleDao.save(
        new DbUserAccessModule()
            .setUser(user)
            .setAccessModule(accessModule)
            .setBypassTime(bypassTime)
            .setCompletionTime(completionTime));
  }

  @Transactional
  public DbUserAccessTier removeUserFromExistingTier(DbUser user, DbAccessTier tier) {
    Optional<DbUserAccessTier> userAccessTierMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, tier);
    assertThat(userAccessTierMaybe).isPresent();

    return userAccessTierDao.save(
        userAccessTierMaybe.get().setTierAccessStatus(TierAccessStatus.DISABLED));
  }

  @Test
  public void testWorkspaceIterator_noEntries() {
    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspaceIIterator_twoAndAHalfBatches() {
    createWorkspaces(5);

    final Iterator<List<ReportingWorkspace>> iterator =
        reportingQueryService.getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingWorkspace> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingWorkspace> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingWorkspace> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspaceIteratorStream() {
    final int numWorkspaces = 5;
    createWorkspaces(numWorkspaces);

    final int totalRows = reportingQueryService.getWorkspacesStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numWorkspaces);

    final long totalBatches = reportingQueryService.getWorkspacesStream().count();
    assertThat(totalBatches).isEqualTo((long) Math.ceil(1.0 * numWorkspaces / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        reportingQueryService
            .getWorkspacesStream()
            .flatMap(List::stream)
            .map(ReportingWorkspace::getWorkspaceId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numWorkspaces);
  }

  @Test
  public void testEmptyStream() {
    workspaceDao.deleteAll();
    final int totalRows = reportingQueryService.getWorkspacesStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(0);

    final long totalBatches = reportingQueryService.getWorkspacesStream().count();
    assertThat(totalBatches).isEqualTo(0);
  }

  @Test
  public void testWorkspaceCount() {
    createWorkspaces(5);
    assertThat(reportingQueryService.getWorkspacesCount()).isEqualTo(5);
  }

  @Test
  public void testUserIterator_twoAndAHalfBatches() {
    createUsers(5);

    final Iterator<List<ReportingUser>> iterator = reportingQueryService.getUserBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingUser> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingUser> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingUser> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testQueryUser() {
    createUsers(1);

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(1);
    userFixture.assertDTOFieldsMatchConstants(stream.stream().findFirst().get().get(0));
  }

  @Test
  public void testQueryUser_disabledTier() {
    final DbUser user = createDbUserWithInstitute();
    addUserToTier(user, registeredTier);
    removeUserFromExistingTier(user, registeredTier);
    entityManager.flush();

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(1);

    ReportingUser reportingUser = stream.stream().findFirst().get().get(0);
    assertThat(reportingUser.getAccessTierShortNames()).isNull();
  }

  @Test
  public void testQueryUser_multiTier() {
    final DbAccessTier tier2 =
        accessTierDao.save(
            new DbAccessTier()
                .setAccessTierId(controlledTier.getAccessTierId())
                .setShortName("tier2")
                .setDisplayName("Tier Two")
                .setAuthDomainName("t2-auth-domain")
                .setAuthDomainGroupEmail("t2-auth-domain@email.com")
                .setServicePerimeter("t2/service/perimeter"));

    final DbUser user = createDbUserWithInstitute();
    addUserToTier(user, registeredTier);
    addUserToTier(user, tier2);

    entityManager.flush();

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());

    // regression test against one row per user/tier pair (i.e. we don't want 2 here)
    assertThat(stream.size()).isEqualTo(1);

    ReportingUser reportingUser = stream.stream().findFirst().get().get(0);
    assertThat(reportingUser.getAccessTierShortNames()).contains(registeredTier.getShortName());
    assertThat(reportingUser.getAccessTierShortNames()).contains(tier2.getShortName());
  }

  @Test
  public void testUserStream_twoAndAHalfBatches() {
    createUsers(5);

    final List<List<ReportingUser>> stream =
        reportingQueryService.getUserStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(3);
  }

  @Test
  public void testQueryInstitution() {
    // A simple test to make sure the query works.
    createInstitutionTierRequirement(dbInstitution);
    final List<ReportingInstitution> institutions = reportingQueryService.getInstitutions();
    assertThat(institutions.size()).isEqualTo(1);
    assertThat(institutions.get(0).getRegisteredTierRequirement())
        .isEqualTo(InstitutionMembershipRequirement.ADDRESSES);
    assertThat(institutions.get(0).getDisplayName()).isEqualTo(dbInstitution.getDisplayName());
  }

  @Test
  public void testQueryNewUserSatisfactionSurvey() {
    final DbUser user = userDao.save(userFixture.createEntity());
    final String additionalInfo = "It's ok.";
    DbNewUserSatisfactionSurvey dbNewUserSatisfactionSurvey =
        newUserSatisfactionSurveyDao.save(
            new DbNewUserSatisfactionSurvey()
                .setUser(user)
                .setSatisfaction(Satisfaction.NEUTRAL)
                .setAdditionalInfo(additionalInfo));

    final List<ReportingNewUserSatisfactionSurvey> newUserSatisfactionSurveys =
        reportingQueryService.getNewUserSatisfactionSurveys(1, 0);

    assertThat(newUserSatisfactionSurveys.size()).isEqualTo(1);
    final ReportingNewUserSatisfactionSurvey newUserSatisfactionSurvey =
        newUserSatisfactionSurveys.get(0);
    assertThat(newUserSatisfactionSurvey.getUserId()).isEqualTo(user.getUserId());
    assertThat(newUserSatisfactionSurvey.getCreated())
        .isEqualTo(offsetDateTimeUtc(dbNewUserSatisfactionSurvey.getCreationTime()));
    assertThat(newUserSatisfactionSurvey.getModified())
        .isEqualTo(offsetDateTimeUtc(dbNewUserSatisfactionSurvey.getCreationTime()));
    assertThat(newUserSatisfactionSurvey.getSatisfaction())
        .isEqualTo(NewUserSatisfactionSurveySatisfaction.NEUTRAL);
    assertThat(newUserSatisfactionSurvey.getAdditionalInfo()).isEqualTo(additionalInfo);
  }

  @Test
  public void testUserCount() {
    createUsers(3);
    assertThat(reportingQueryService.getUserCount()).isEqualTo(3);
  }

  @Test
  public void testCohortIterator_twoAndAHalfBatches() {
    createCohorts(5);

    final Iterator<List<ReportingCohort>> iterator =
        reportingQueryService.getCohortsBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingCohort> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingCohort> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingCohort> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testCohortStream_twoAndAHalfBatches() {
    createCohorts(5);

    final List<List<ReportingCohort>> stream =
        reportingQueryService.getCohortsStream().collect(Collectors.toList());
    assertThat(stream.size()).isEqualTo(3);
  }

  @Test
  public void testCohortsCount() {
    createCohorts(3);
    assertThat(reportingQueryService.getCohortsCount()).isEqualTo(3);
  }

  @Test
  public void testNewUserSatisfactionSurveysIterator() {
    createNewUserSatisfactionSurveys(5);

    final Iterator<List<ReportingNewUserSatisfactionSurvey>> iterator =
        reportingQueryService.getNewUserSatisfactionSurveyBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    final List<ReportingNewUserSatisfactionSurvey> batch1 = iterator.next();
    assertThat(batch1).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingNewUserSatisfactionSurvey> batch2 = iterator.next();
    assertThat(batch2).hasSize(BATCH_SIZE);

    assertThat(iterator.hasNext()).isTrue();
    final List<ReportingNewUserSatisfactionSurvey> batch3 = iterator.next();
    assertThat(batch3).hasSize(1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testNewUserSatisfactionSurveysStream() {
    final int numNewUserSatisfactionSurveys = 5;
    createNewUserSatisfactionSurveys(numNewUserSatisfactionSurveys);

    final int totalRows =
        reportingQueryService.getNewUserSatisfactionSurveysStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numNewUserSatisfactionSurveys);

    final long totalBatches = reportingQueryService.getNewUserSatisfactionSurveysStream().count();
    assertThat(totalBatches)
        .isEqualTo((long) Math.ceil(1.0 * numNewUserSatisfactionSurveys / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        reportingQueryService
            .getNewUserSatisfactionSurveysStream()
            .flatMap(List::stream)
            .map(ReportingNewUserSatisfactionSurvey::getId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numNewUserSatisfactionSurveys);
  }

  @Test
  public void testNewUserSatisfactionSurveysCount() {
    createNewUserSatisfactionSurveys(5);
    assertThat(reportingQueryService.getNewUserSatisfactionSurveysCount()).isEqualTo(5);
  }

  private void createWorkspaces(int count) {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    for (int i = 0; i < count; ++i) {
      createDbWorkspace(user, cdrVersion);
    }
    entityManager.flush();
  }

  private void createUsers(int count) {
    for (int i = 0; i < count; ++i) {
      final DbUser user = createDbUserWithInstitute();
      addUserToTier(user, registeredTier);
      addUserAccessModule(
          user,
          twoFactorAuthModule,
          USER__TWO_FACTOR_AUTH_BYPASS_TIME,
          USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
      addUserAccessModule(
          user,
          rtTrainingModule,
          USER__COMPLIANCE_TRAINING_BYPASS_TIME,
          USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
      addUserAccessModule(
          user, eRACommonsModule, USER__ERA_COMMONS_BYPASS_TIME, USER__ERA_COMMONS_COMPLETION_TIME);
      addUserAccessModule(
          user, identityModule, USER__IDENTITY_BYPASS_TIME, USER__IDENTITY_COMPLETION_TIME);
      addUserAccessModule(
          user,
          rasLoginGovModule,
          USER__RAS_LOGIN_GOV_BYPASS_TIME,
          USER__RAS_LOGIN_GOV_COMPLETION_TIME);
      addUserAccessModule(
          user,
          duccModule,
          USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME,
          USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME);
    }
    entityManager.flush();
  }

  private void createCohorts(int count) {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    final DbWorkspace dbWorkspace = createDbWorkspace(user, cdrVersion);

    for (int i = 0; i < count; ++i) {
      createCohort(user, dbWorkspace);
    }
    entityManager.flush();
  }

  private void createNewUserSatisfactionSurveys(int count) {
    for (int i = 0; i < count; ++i) {
      final DbUser user = userDao.save(userFixture.createEntity());
      newUserSatisfactionSurveyDao.save(
          new DbNewUserSatisfactionSurvey()
              .setUser(user)
              .setSatisfaction(Satisfaction.NEUTRAL)
              .setAdditionalInfo("It's ok."));
    }
    entityManager.flush();
  }

  public void createInstitutionTierRequirement(DbInstitution institution) {
    institutionTierRequirementDao.save(
        new DbInstitutionTierRequirement()
            .setAccessTier(registeredTier)
            .setInstitution(institution)
            .setMembershipRequirement(MembershipRequirement.ADDRESSES)
            .setEraRequired(true));
    institutionTierRequirementDao.save(
        new DbInstitutionTierRequirement()
            .setAccessTier(controlledTier)
            .setInstitution(institution)
            .setMembershipRequirement(MembershipRequirement.DOMAINS)
            .setEraRequired(false));
  }
}
