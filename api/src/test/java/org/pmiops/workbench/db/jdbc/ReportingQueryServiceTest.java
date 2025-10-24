package org.pmiops.workbench.db.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__TWO_FACTOR_AUTH_BYPASS_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__TWO_FACTOR_AUTH_COMPLETION_TIME;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import jakarta.inject.Provider;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
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
import org.pmiops.workbench.db.model.DbDemographicSurveyV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbEducationV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbEthnicCategory;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbGenderIdentityV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbSexAtBirthV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbSexualOrientationV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbYesNoPreferNot;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.DbGeneralDiscoverySource;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture;
import org.pmiops.workbench.utils.BigQueryUtils;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test the unique ReportingQueryService, which bypasses Spring in favor of low-level JDBC queries.
 * This means we need real DAOs.
 */
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingQueryServiceTest {

  public static final int BATCH_SIZE = 2;

  private static WorkbenchConfig workbenchConfig;

  @Autowired private ReportingQueryService reportingQueryService;

  // It's necessary to bring in several Dao classes, since we aim to populate join tables
  // that have neither entities of their own nor stand-alone DAOs.
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private InstitutionDao institutionDao;
  @Autowired private InstitutionTierRequirementDao institutionTierRequirementDao;
  @Autowired private NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserDao userDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("REPORTING_USER_TEST_FIXTURE")
  ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @MockBean private BigQueryService bigQueryService;
  @MockBean Provider<WorkbenchConfig> workbenchConfigProvider;

  @Import({
    FakeClockConfiguration.class,
    ReportingQueryServiceImpl.class,
    ReportingUserFixture.class,
    ReportingTestConfig.class
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  private DbInstitution dbInstitution;
  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  private DbAccessModule twoFactorAuthModule;
  private DbAccessModule rtTrainingModule;
  private DbAccessModule eRACommonsModule;
  private DbAccessModule identityModule;
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
    duccModule = accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.reporting.maxRowsPerInsert = BATCH_SIZE;
    workbenchConfig.billing.accountId = "initial-credits";
  }

  @Transactional
  public DbCohort createCohort(DbUser user1, DbWorkspace workspace1) {
    return cohortDao.save(ReportingTestUtils.createDbCohort(user1, workspace1));
  }

  @Transactional
  public DbWorkspace createDbWorkspace(DbUser user1, DbCdrVersion cdrVersion1) {
    final long initialWorkspaceCount = workspaceDao.count();
    DbWorkspace workspace1 =
        workspaceDao.save(ReportingTestUtils.createDbWorkspace(user1, cdrVersion1));
    assertThat(workspaceDao.count()).isEqualTo(initialWorkspaceCount + 1);
    return workspace1;
  }

  @Transactional
  public DbWorkspace createDbWorkspaceWithDemographicData(
      DbUser user1, DbCdrVersion cdrVersion1) {
    final long initialWorkspaceCount = workspaceDao.count();
    DbWorkspace workspace1 =
        workspaceDao.save(ReportingTestUtils.createDbWorkspaceWithDemographicData(user1, cdrVersion1));
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
    DbUser user = userDao.save(userFixture.createEntity());
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

    final Iterator<List<ReportingWorkspace>> iterator = getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    List<ReportingWorkspace> firstBatch = iterator.next();
    assertThat(firstBatch).hasSize(1);
    assertThat(firstBatch.get(0).getName()).isEqualTo(workspace.getName());
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspace_withDemographicData() {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    DbWorkspace workspace = createDbWorkspaceWithDemographicData(user, cdrVersion);

    final Iterator<List<ReportingWorkspace>> iterator = getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    List<ReportingWorkspace> firstBatch = iterator.next();
    assertThat(firstBatch).hasSize(1);
    assertThat(firstBatch.get(0).getName()).isEqualTo(workspace.getName());
    assertThat(iterator.hasNext()).isFalse();

    // verify demographic data fields
    assertThat(firstBatch.get(0).isFocusOnUnderrepresentedPopulations()).isTrue();
    assertThat(firstBatch.get(0).getWorkspaceDemographic()).isNotNull();

    WorkspaceDemographic demographic = firstBatch.get(0).getWorkspaceDemographic();
    assertThat(demographic.getAge().size()).isEqualTo(1);
    assertThat(demographic.getAge().get(0)).isEqualTo(WorkspaceDemographic.AgeEnum.AGE_65_74);
    assertThat(demographic.getRaceEthnicity().size()).isEqualTo(1);
    assertThat(demographic.getRaceEthnicity().get(0)).isEqualTo(WorkspaceDemographic.RaceEthnicityEnum.ASIAN);
    assertThat(demographic.getSexAtBirth()).isEqualTo(WorkspaceDemographic.SexAtBirthEnum.UNSET);
    assertThat(demographic.getGenderIdentity()).isEqualTo(WorkspaceDemographic.GenderIdentityEnum.UNSET);
    assertThat(demographic.getSexualOrientation()).isEqualTo(WorkspaceDemographic.SexualOrientationEnum.UNSET);
    assertThat(demographic.getGeography()).isEqualTo(WorkspaceDemographic.GeographyEnum.RURAL);
    assertThat(demographic.getDisabilityStatus()).isEqualTo(WorkspaceDemographic.DisabilityStatusEnum.UNSET);
    assertThat(demographic.getAccessToCare()).isEqualTo(WorkspaceDemographic.AccessToCareEnum.UNSET);
    assertThat(demographic.getEducationLevel()).isEqualTo(WorkspaceDemographic.EducationLevelEnum.UNSET);
    assertThat(demographic.getIncomeLevel()).isEqualTo(WorkspaceDemographic.IncomeLevelEnum.UNSET);
  }

  @Test
  public void testWorkspace_withoutDemographicData() {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    DbWorkspace workspace = createDbWorkspace(user, cdrVersion);

    final Iterator<List<ReportingWorkspace>> iterator = getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isTrue();

    List<ReportingWorkspace> firstBatch = iterator.next();
    assertThat(firstBatch).hasSize(1);
    assertThat(firstBatch.get(0).getName()).isEqualTo(workspace.getName());
    assertThat(iterator.hasNext()).isFalse();

    // verify demographic data is not set
    assertThat(firstBatch.get(0).isFocusOnUnderrepresentedPopulations()).isFalse();
    assertThat(firstBatch.get(0).getWorkspaceDemographic()).isNull();
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
    final Iterator<List<ReportingWorkspace>> iterator = getWorkspaceBatchIterator();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  public void testWorkspaceIterator_twoAndAHalfBatches() {
    createWorkspaces(5);

    final Iterator<List<ReportingWorkspace>> iterator = getWorkspaceBatchIterator();
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

    final int totalRows = getBatchedWorkspaceStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numWorkspaces);

    final long totalBatches = getBatchedWorkspaceStream().count();
    assertThat(totalBatches).isEqualTo((long) Math.ceil(1.0 * numWorkspaces / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        getBatchedWorkspaceStream()
            .flatMap(List::stream)
            .map(ReportingWorkspace::getWorkspaceId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numWorkspaces);
  }

  @Test
  public void testWorkspaceIteratorStream_withDeleted() {
    final int numWorkspaces = 5;
    List<DbWorkspace> workspaces = createWorkspaces(numWorkspaces);
    workspaceDao.save(
        workspaces.get(0).setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED));
    entityManager.flush();

    final int totalRows = getBatchedWorkspaceStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numWorkspaces);

    final long totalBatches = getBatchedWorkspaceStream().count();
    assertThat(totalBatches).isEqualTo((long) Math.ceil(1.0 * numWorkspaces / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        getBatchedWorkspaceStream()
            .flatMap(List::stream)
            .map(ReportingWorkspace::getWorkspaceId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numWorkspaces);
  }

  @Test
  public void testEmptyStream() {
    workspaceDao.deleteAll();
    final int totalRows = getBatchedWorkspaceStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(0);

    final long totalBatches = getBatchedWorkspaceStream().count();
    assertThat(totalBatches).isEqualTo(0);
  }

  @Test
  public void testWorkspaceCount() {
    createWorkspaces(5);
    assertThat(reportingQueryService.getTableRowCount("workspace")).isEqualTo(5);
  }

  @Test
  public void testWorkspaceCount_withDeleted() {
    List<DbWorkspace> workspaces = createWorkspaces(5);
    workspaceDao.save(
        workspaces.get(0).setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED));
    entityManager.flush();
    assertThat(reportingQueryService.getTableRowCount("workspace")).isEqualTo(5);
  }

  @Test
  public void testUserIterator_twoAndAHalfBatches() {
    createUsers(5);

    final Iterator<List<ReportingUser>> iterator = getUserBatchIterator();
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

    final List<List<ReportingUser>> batches = getBatchedUserStream().toList();
    assertThat(batches.size()).isEqualTo(1);
    userFixture.assertDTOFieldsMatchConstants(batches.stream().findFirst().get().get(0));
  }

  @Test
  public void testQueryUser_disabledTier() {
    final DbUser user = createDbUserWithInstitute();
    addUserToTier(user, registeredTier);
    removeUserFromExistingTier(user, registeredTier);
    entityManager.flush();

    final List<List<ReportingUser>> batches = getBatchedUserStream().toList();
    assertThat(batches.size()).isEqualTo(1);

    ReportingUser reportingUser = batches.stream().findFirst().get().get(0);
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

    final List<List<ReportingUser>> batches = getBatchedUserStream().toList();

    // regression test against one row per user/tier pair (i.e. we don't want 2 here)
    assertThat(batches.size()).isEqualTo(1);

    ReportingUser reportingUser = batches.stream().findFirst().get().get(0);
    assertThat(reportingUser.getAccessTierShortNames()).contains(registeredTier.getShortName());
    assertThat(reportingUser.getAccessTierShortNames()).contains(tier2.getShortName());
  }

  @Test
  public void testUserStream_twoAndAHalfBatches() {
    createUsers(5);
    assertThat(getBatchedUserStream().count()).isEqualTo(3);
  }

  @Test
  public void testQueryUser_withDsv2Fields() {
    final DbUser user = createDbUserWithInstitute();
    addUserToTier(user, registeredTier);
    createDemographicSurveyV2(user);

    entityManager.flush();

    final List<List<ReportingUser>> batches = getBatchedUserStream().toList();
    assertThat(batches.size()).isEqualTo(1);

    ReportingUser reportingUser = batches.stream().findFirst().get().get(0);

    // Verify dsv2 fields are populated
    assertThat(reportingUser.getDsv2CompletionTime()).isNotNull();
    assertThat(reportingUser.getDsv2DisabilityConcentrating()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2DisabilityDressing()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2DisabilityErrands()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2DisabilityHearing()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2DisabilityOtherText()).isEqualTo("No other disabilities");
    assertThat(reportingUser.getDsv2DisabilitySeeing()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2DisabilityWalking()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2Disadvantaged()).isEqualTo("NO");
    assertThat(reportingUser.getDsv2Education()).isEqualTo("COLLEGE_GRADUATE");
    assertThat(reportingUser.getDsv2EthnicityAiAnOtherText()).isEqualTo("AI/AN other text");
    assertThat(reportingUser.getDsv2EthnicityAsianOtherText()).isEqualTo("Asian other text");
    assertThat(reportingUser.getDsv2EthnicityBlackOtherText()).isEqualTo("Black other text");
    assertThat(reportingUser.getDsv2EthnicityHispanicOtherText()).isEqualTo("Hispanic other text");
    assertThat(reportingUser.getDsv2EthnicityMeNaOtherText()).isEqualTo("ME/NA other text");
    assertThat(reportingUser.getDsv2EthnicityNhPiOtherText()).isEqualTo("NH/PI other text");
    assertThat(reportingUser.getDsv2EthnicityOtherText()).isEqualTo("Other ethnicity text");
    assertThat(reportingUser.getDsv2EthnicityWhiteOtherText()).isEqualTo("White other text");
    assertThat(reportingUser.getDsv2GenderOtherText()).isEqualTo("Gender other text");
    assertThat(reportingUser.getDsv2OrientationOtherText()).isEqualTo("Orientation other text");
    assertThat(reportingUser.getDsv2SexAtBirth()).isEqualTo("MALE");
    assertThat(reportingUser.getDsv2SexAtBirthOtherText()).isEqualTo("Sex at birth other text");
    assertThat(reportingUser.getDsv2SurveyComments()).isEqualTo("Test survey comments");
    assertThat(reportingUser.getDsv2YearOfBirth()).isEqualTo(1990);
    assertThat(reportingUser.isDsv2YearOfBirthPreferNot()).isEqualTo(false);
    assertThat(reportingUser.getDsv2EthnicCategory()).isEqualTo("WHITE");
    assertThat(reportingUser.getDsv2GenderIdentity()).isEqualTo("MAN");
    assertThat(reportingUser.getDsv2SexualOrientation()).isEqualTo("STRAIGHT");
  }

  @Test
  public void testQueryUser_withMultipleDsv2Values() {
    final DbUser user = createDbUserWithInstitute();
    addUserToTier(user, registeredTier);

    // Create dsv2 with multiple values for multi-value fields
    DbDemographicSurveyV2 dsv2 = new DbDemographicSurveyV2();
    dsv2.setUser(user);
    dsv2.setCompletionTime(Timestamp.from(Instant.now()));
    dsv2.setEducation(DbEducationV2.MASTER);
    dsv2.setSexAtBirth(DbSexAtBirthV2.FEMALE);
    dsv2.setYearOfBirth(1985L);
    dsv2.setYearOfBirthPreferNot(false);

    // Set multiple values for multi-value fields
    dsv2.setEthnicCategory(Set.of(DbEthnicCategory.WHITE, DbEthnicCategory.ASIAN));
    dsv2.setGenderIdentity(Set.of(DbGenderIdentityV2.WOMAN, DbGenderIdentityV2.NON_BINARY));
    dsv2.setSexualOrientation(Set.of(DbSexualOrientationV2.STRAIGHT));

    entityManager.merge(dsv2);
    entityManager.flush();

    final List<List<ReportingUser>> batches = getBatchedUserStream().toList();
    assertThat(batches.size()).isEqualTo(1);

    ReportingUser reportingUser = batches.stream().findFirst().get().get(0);

    // Verify multi-value fields are comma-separated
    assertThat(reportingUser.getDsv2EthnicCategory()).contains("WHITE");
    assertThat(reportingUser.getDsv2EthnicCategory()).contains("ASIAN");
    assertThat(reportingUser.getDsv2EthnicCategory()).contains(",");

    assertThat(reportingUser.getDsv2GenderIdentity()).contains("WOMAN");
    assertThat(reportingUser.getDsv2GenderIdentity()).contains("NON_BINARY");
    assertThat(reportingUser.getDsv2GenderIdentity()).contains(",");
  }

  @Test
  public void testQueryInstitution() {
    // A simple test to make sure the query works.
    createInstitutionTierRequirement(dbInstitution);
    final List<ReportingInstitution> institutions = reportingQueryService.getInstitutionBatch(1, 0);
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
        reportingQueryService.getNewUserSatisfactionSurveyBatch(1, 0);

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
  public void testQueryUserGeneralDiscoverySource() {
    final DbUser user = userFixture.createEntity();
    user.setGeneralDiscoverySources(
            new HashSet<>(
                Arrays.asList(
                    DbGeneralDiscoverySource.FRIENDS_OR_COLLEAGUES,
                    DbGeneralDiscoverySource.OTHER_WEBSITE,
                    DbGeneralDiscoverySource.OTHER)))
        .setGeneralDiscoverySourceOtherText("other text");
    userDao.save(user);
    entityManager.flush();

    final List<ReportingUserGeneralDiscoverySource> userGeneralDiscoverySources =
        reportingQueryService.getUserGeneralDiscoverySourceBatch(5, 0);

    assertThat(userGeneralDiscoverySources)
        .containsExactly(
            new ReportingUserGeneralDiscoverySource()
                .userId(user.getUserId())
                .answer(DbGeneralDiscoverySource.FRIENDS_OR_COLLEAGUES.toString()),
            new ReportingUserGeneralDiscoverySource()
                .userId(user.getUserId())
                .answer(DbGeneralDiscoverySource.OTHER_WEBSITE.toString()),
            new ReportingUserGeneralDiscoverySource()
                .userId(user.getUserId())
                .answer(DbGeneralDiscoverySource.OTHER.toString())
                .otherText("other text"));
  }

  @Test
  public void testQueryUserPartnerDiscoverySource() {
    final DbUser user = userFixture.createEntity();
    user.setPartnerDiscoverySources(
            new HashSet<>(
                Arrays.asList(
                    PartnerDiscoverySource.PYXIS_PARTNERS,
                    PartnerDiscoverySource.ALL_OF_US_RESEARCH_PROGRAM_STAFF,
                    PartnerDiscoverySource.OTHER)))
        .setPartnerDiscoverySourceOtherText("other text");
    userDao.save(user);
    entityManager.flush();

    final List<ReportingUserPartnerDiscoverySource> userPartnerDiscoverySources =
        reportingQueryService.getUserPartnerDiscoverySourceBatch(5, 0);

    assertThat(userPartnerDiscoverySources)
        .containsExactly(
            new ReportingUserPartnerDiscoverySource()
                .userId(user.getUserId())
                .answer(PartnerDiscoverySource.PYXIS_PARTNERS.toString()),
            new ReportingUserPartnerDiscoverySource()
                .userId(user.getUserId())
                .answer(PartnerDiscoverySource.ALL_OF_US_RESEARCH_PROGRAM_STAFF.toString()),
            new ReportingUserPartnerDiscoverySource()
                .userId(user.getUserId())
                .answer(PartnerDiscoverySource.OTHER.toString())
                .otherText("other text"));
  }

  @Test
  public void testGetRowCount() {
    createUsers(3);
    assertThat(reportingQueryService.getTableRowCount("user")).isEqualTo(3);
  }

  @Test
  public void testCohortIterator_twoAndAHalfBatches() {
    createCohorts(5);

    final Iterator<List<ReportingCohort>> iterator = getCohortsBatchIterator();
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
    assertThat(Streams.stream(getCohortsBatchIterator()).count()).isEqualTo(3);
  }

  @Test
  public void testNewUserSatisfactionSurveysIterator() {
    createNewUserSatisfactionSurveys(5);

    final Iterator<List<ReportingNewUserSatisfactionSurvey>> iterator =
        getNewUserSatisfactionSurveyBatchIterator();
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

    final int totalRows = getBatchedNewUserSatisfactionSurveyStream().mapToInt(List::size).sum();
    assertThat(totalRows).isEqualTo(numNewUserSatisfactionSurveys);

    final long totalBatches = getBatchedNewUserSatisfactionSurveyStream().count();
    assertThat(totalBatches)
        .isEqualTo((long) Math.ceil(1.0 * numNewUserSatisfactionSurveys / BATCH_SIZE));

    // verify that we get all of them and they're distinct in terms of their PKs
    final Set<Long> ids =
        getBatchedNewUserSatisfactionSurveyStream()
            .flatMap(List::stream)
            .map(ReportingNewUserSatisfactionSurvey::getId)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(ids).hasSize(numNewUserSatisfactionSurveys);
  }

  @Test
  public void testQueryLeonardoAppUsage() {
    workbenchConfig.reporting.exportTerraDataWarehouse = true;
    workbenchConfig.reporting.terraWarehouseLeoAppUsageTableId = "app_usage_table";
    workbenchConfig.reporting.terraWarehouseLeoAppTableId = "app_table";

    Instant now = Instant.now();

    Field idField = Field.of("appId", LegacySQLTypeName.STRING);
    FieldValue idValue = FieldValue.of(Attribute.PRIMITIVE, "123");

    Field appNameField = Field.of("appName", LegacySQLTypeName.STRING);
    FieldValue appNameValue = FieldValue.of(Attribute.PRIMITIVE, "all-of-us-123-sas-esdw");

    Field statusField = Field.of("status", LegacySQLTypeName.STRING);
    FieldValue statusValue = FieldValue.of(Attribute.PRIMITIVE, "DELETED");

    Field creatorField = Field.of("creator", LegacySQLTypeName.STRING);
    FieldValue creatorValue = FieldValue.of(Attribute.PRIMITIVE, "user@email.com");

    Field customEnvironmentVariablesField =
        Field.of("customEnvironmentVariables", LegacySQLTypeName.STRING);
    FieldValue customEnvironmentVariablesValue = FieldValue.of(Attribute.PRIMITIVE, "env vars");

    Field createdDateField = Field.of("createdDate", LegacySQLTypeName.TIMESTAMP);
    FieldValue createdDateValue =
        FieldValue.of(Attribute.PRIMITIVE, String.valueOf(now.plusSeconds(1).getEpochSecond()));

    Field destroyedDateDateField = Field.of("destroyedDate", LegacySQLTypeName.TIMESTAMP);
    FieldValue destroyedDateValue =
        FieldValue.of(Attribute.PRIMITIVE, String.valueOf(now.plusSeconds(2).getEpochSecond()));

    Field startTimeField = Field.of("startTime", LegacySQLTypeName.TIMESTAMP);
    FieldValue startTimeValue =
        FieldValue.of(Attribute.PRIMITIVE, String.valueOf(now.plusSeconds(3).getEpochSecond()));

    Field stopTimeField = Field.of("stopTime", LegacySQLTypeName.TIMESTAMP);
    FieldValue stopTimeValue =
        FieldValue.of(Attribute.PRIMITIVE, String.valueOf(now.plusSeconds(4).getEpochSecond()));

    Schema s =
        Schema.of(
            idField,
            appNameField,
            statusField,
            creatorField,
            customEnvironmentVariablesField,
            createdDateField,
            destroyedDateDateField,
            startTimeField,
            stopTimeField);

    List<FieldValueList> tableRows =
        List.of(
            FieldValueList.of(
                Arrays.asList(
                    idValue,
                    appNameValue,
                    statusValue,
                    creatorValue,
                    customEnvironmentVariablesValue,
                    createdDateValue,
                    destroyedDateValue,
                    startTimeValue,
                    stopTimeValue)));

    TableResult tableResult = BigQueryUtils.newTableResult(s, tableRows);
    when(bigQueryService.executeQuery(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    assertThat(reportingQueryService.getLeonardoAppUsageBatch(10, 0))
        .containsExactly(
            new ReportingLeonardoAppUsage()
                .appId(123L)
                .appName("all-of-us-123-sas-esdw")
                .appType("SAS")
                .creator("user@email.com")
                .status("DELETED")
                .createdDate(now.plusSeconds(1).atOffset(ZoneOffset.UTC).withNano(0))
                .destroyedDate(now.plusSeconds(2).atOffset(ZoneOffset.UTC).withNano(0))
                .startTime(now.plusSeconds(3).atOffset(ZoneOffset.UTC).withNano(0))
                .stopTime(now.plusSeconds(4).atOffset(ZoneOffset.UTC).withNano(0))
                .environmentVariables("env vars"));
  }

  private Iterator<List<ReportingWorkspace>> getWorkspaceBatchIterator() {
    return reportingQueryService.getBatchIterator(
        reportingQueryService::getWorkspaceBatch, BATCH_SIZE);
  }

  private Iterator<List<ReportingUser>> getUserBatchIterator() {
    return reportingQueryService.getBatchIterator(reportingQueryService::getUserBatch, BATCH_SIZE);
  }

  private Iterator<List<ReportingCohort>> getCohortsBatchIterator() {
    return reportingQueryService.getBatchIterator(
        reportingQueryService::getCohortBatch, BATCH_SIZE);
  }

  private Iterator<List<ReportingNewUserSatisfactionSurvey>>
      getNewUserSatisfactionSurveyBatchIterator() {
    return reportingQueryService.getBatchIterator(
        reportingQueryService::getNewUserSatisfactionSurveyBatch, BATCH_SIZE);
  }

  private Stream<List<ReportingUser>> getBatchedUserStream() {
    return reportingQueryService.getBatchedStream(reportingQueryService::getUserBatch, BATCH_SIZE);
  }

  private Stream<List<ReportingNewUserSatisfactionSurvey>>
      getBatchedNewUserSatisfactionSurveyStream() {
    return reportingQueryService.getBatchedStream(
        reportingQueryService::getNewUserSatisfactionSurveyBatch, BATCH_SIZE);
  }

  private Stream<List<ReportingWorkspace>> getBatchedWorkspaceStream() {
    return reportingQueryService.getBatchedStream(
        reportingQueryService::getWorkspaceBatch, BATCH_SIZE);
  }

  private List<DbWorkspace> createWorkspaces(int count) {
    final DbUser user = createDbUserWithInstitute();
    final DbCdrVersion cdrVersion = createCdrVersion(registeredTier);
    final List<DbWorkspace> workspaces = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      workspaces.add(createDbWorkspace(user, cdrVersion));
    }
    entityManager.flush();
    return workspaces;
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
          duccModule,
          USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME,
          USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME);
      createDemographicSurveyV2(user);
    }
    entityManager.flush();
  }

  @Transactional
  public DbDemographicSurveyV2 createDemographicSurveyV2(DbUser user) {
    DbDemographicSurveyV2 dsv2 = new DbDemographicSurveyV2();
    dsv2.setUser(user);
    dsv2.setCompletionTime(Timestamp.from(Instant.now()));
    dsv2.setDisabilityConcentrating(DbYesNoPreferNot.NO);
    dsv2.setDisabilityDressing(DbYesNoPreferNot.NO);
    dsv2.setDisabilityErrands(DbYesNoPreferNot.NO);
    dsv2.setDisabilityHearing(DbYesNoPreferNot.NO);
    dsv2.setDisabilityOtherText("No other disabilities");
    dsv2.setDisabilitySeeing(DbYesNoPreferNot.NO);
    dsv2.setDisabilityWalking(DbYesNoPreferNot.NO);
    dsv2.setDisadvantaged(DbYesNoPreferNot.NO);
    dsv2.setEducation(DbEducationV2.COLLEGE_GRADUATE);
    dsv2.setEthnicityAiAnOtherText("AI/AN other text");
    dsv2.setEthnicityAsianOtherText("Asian other text");
    dsv2.setEthnicityBlackOtherText("Black other text");
    dsv2.setEthnicityHispanicOtherText("Hispanic other text");
    dsv2.setEthnicityMeNaOtherText("ME/NA other text");
    dsv2.setEthnicityNhPiOtherText("NH/PI other text");
    dsv2.setEthnicityOtherText("Other ethnicity text");
    dsv2.setEthnicityWhiteOtherText("White other text");
    dsv2.setGenderOtherText("Gender other text");
    dsv2.setOrientationOtherText("Orientation other text");
    dsv2.setSexAtBirth(DbSexAtBirthV2.MALE);
    dsv2.setSexAtBirthOtherText("Sex at birth other text");
    dsv2.setSurveyComments("Test survey comments");
    dsv2.setYearOfBirth(1990L);
    dsv2.setYearOfBirthPreferNot(false);

    dsv2.setEthnicCategory(Set.of(DbEthnicCategory.WHITE));
    dsv2.setGenderIdentity(Set.of(DbGenderIdentityV2.MAN));
    dsv2.setSexualOrientation(Set.of(DbSexualOrientationV2.STRAIGHT));

    dsv2 = entityManager.merge(dsv2);

    return dsv2;
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
            .setMembershipRequirement(MembershipRequirement.ADDRESSES));
    institutionTierRequirementDao.save(
        new DbInstitutionTierRequirement()
            .setAccessTier(controlledTier)
            .setInstitution(institution)
            .setMembershipRequirement(MembershipRequirement.DOMAINS));
  }
}
