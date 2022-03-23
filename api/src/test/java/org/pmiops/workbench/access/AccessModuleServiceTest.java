package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.access.AccessModuleServiceImpl.deriveExpirationTimestamp;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class AccessModuleServiceTest {
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessModuleService accessModuleService;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private UserDao userDao;

  @MockBean private UserServiceAuditor mockUserServiceAuditAdapter;
  private static DbUser user;
  private static WorkbenchConfig config;
  private static List<DbAccessModule> accessModules;

  @Import({
    FakeClockConfiguration.class,
    AccessModuleServiceImpl.class,
    UserAccessModuleMapperImpl.class,
    CommonMappers.class,
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig config() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setup() {
    config = WorkbenchConfig.createEmptyConfig();
    config.access.enableComplianceTraining = true;
    config.access.enableEraCommons = true;
    config.access.currentDuccVersions = ImmutableList.of(10, 11); // arbitrary for test

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);

    user = new DbUser();
    user.setUsername("user");
    user.setDuccAgreement(
        TestMockFactory.createDuccAgreement(user, 10, FakeClockConfiguration.NOW));
    user = userDao.save(user);
  }

  @Test
  public void testBypassSuccess_insertNewEntity() {
    assertThat(userAccessModuleDao.getAllByUser(user)).isEmpty();
    accessModuleService.updateBypassTime(user.getUserId(), AccessModule.TWO_FACTOR_AUTH, true);
    List<DbUserAccessModule> userAccessModule = userAccessModuleDao.getAllByUser(user);
    assertThat(userAccessModule.size()).isEqualTo(1);
    assertThat(userAccessModule.get(0).getAccessModule().getName())
        .isEqualTo(DbAccessModuleName.TWO_FACTOR_AUTH);
    assertThat(userAccessModule.get(0).getBypassTime()).isEqualTo(FakeClockConfiguration.NOW);

    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            user.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH,
            Optional.empty(),
            nullableTimestampToOptionalInstant(FakeClockConfiguration.NOW));
  }

  @Test
  public void testBypassSuccess_updateExistingEntity() {
    // A TWO_FACTOR_AUTH module exists in DbUserAccessModule
    DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    Timestamp existingBypasstime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
    DbUserAccessModule existingDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setUser(user)
            .setBypassTime(existingBypasstime);
    userAccessModuleDao.save(existingDbUserAccessModule);
    assertThat(userAccessModuleDao.getAllByUser(user).size()).isEqualTo(1);
    accessModuleService.updateBypassTime(user.getUserId(), AccessModule.TWO_FACTOR_AUTH, true);

    List<DbUserAccessModule> userAccessModule = userAccessModuleDao.getAllByUser(user);
    assertThat(userAccessModule.size()).isEqualTo(1);
    assertThat(userAccessModule.get(0).getAccessModule().getName())
        .isEqualTo(DbAccessModuleName.TWO_FACTOR_AUTH);
    assertThat(userAccessModule.get(0).getBypassTime()).isEqualTo(FakeClockConfiguration.NOW);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            user.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH,
            nullableTimestampToOptionalInstant(existingBypasstime),
            nullableTimestampToOptionalInstant(FakeClockConfiguration.NOW));
  }

  @Test
  public void testUnBypassSuccess_insertNewEntity() {
    assertThat(userAccessModuleDao.getAllByUser(user)).isEmpty();
    accessModuleService.updateBypassTime(user.getUserId(), AccessModule.TWO_FACTOR_AUTH, false);

    List<DbUserAccessModule> userAccessModule = userAccessModuleDao.getAllByUser(user);
    assertThat(userAccessModule.size()).isEqualTo(1);
    assertThat(userAccessModule.get(0).getAccessModule().getName())
        .isEqualTo(DbAccessModuleName.TWO_FACTOR_AUTH);
    assertThat(userAccessModule.get(0).getBypassTime()).isNull();
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            user.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH,
            Optional.empty(),
            Optional.empty());
  }

  @Test
  public void testUnBypassSuccess_updateExistingEntity() {
    // A TWO_FACTOR_AUTH module exists in DbUserAccessModule
    DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    Timestamp existingBypasstime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
    DbUserAccessModule existingDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setUser(user)
            .setBypassTime(existingBypasstime);
    userAccessModuleDao.save(existingDbUserAccessModule);
    assertThat(userAccessModuleDao.getAllByUser(user).size()).isEqualTo(1);
    accessModuleService.updateBypassTime(user.getUserId(), AccessModule.TWO_FACTOR_AUTH, false);

    List<DbUserAccessModule> userAccessModule = userAccessModuleDao.getAllByUser(user);
    assertThat(userAccessModule.size()).isEqualTo(1);
    assertThat(userAccessModule.get(0).getAccessModule().getName())
        .isEqualTo(DbAccessModuleName.TWO_FACTOR_AUTH);
    assertThat(userAccessModule.get(0).getBypassTime()).isNull();
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            user.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH,
            nullableTimestampToOptionalInstant(existingBypasstime),
            Optional.empty());
  }

  @Test
  public void testBypassFail_moduleNotBypassable() {
    assertThrows(
        ForbiddenException.class,
        () ->
            accessModuleService.updateBypassTime(
                user.getUserId(), AccessModule.PROFILE_CONFIRMATION, true));
  }

  @Test
  public void testGetClientAccessModuleStatus() {
    Instant now = Instant.ofEpochMilli(FakeClockConfiguration.NOW_TIME);
    long expiryDays = 365L;
    config.access.renewal.expiryDays = expiryDays;
    DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    DbAccessModule rtTrainingModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();
    DbAccessModule profileConfirmModule =
        accessModuleDao.findOneByName(DbAccessModuleName.PROFILE_CONFIRMATION).get();
    DbAccessModule publicationModule =
        accessModuleDao.findOneByName(DbAccessModuleName.PUBLICATION_CONFIRMATION).get();
    DbAccessModule ducc =
        accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();

    // 2FA module: Module is not expirable, so no expiration date present.
    Timestamp twoFactorCompletionTime = Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    DbUserAccessModule twoFactorAuthUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setUser(user)
            .setCompletionTime(twoFactorCompletionTime);
    AccessModuleStatus expected2FAModuleStatus =
        new AccessModuleStatus()
            .moduleName(AccessModule.TWO_FACTOR_AUTH)
            .completionEpochMillis(twoFactorCompletionTime.getTime());

    // RT Training module: Completion time + expiryDays is 10 days ahead current time, but the
    // module was bypassed, so no expiration time.
    Timestamp rtTrainingCompletionTime =
        Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    Timestamp rtTrainingBypassTime = Timestamp.from(now);
    DbUserAccessModule rtTrainingAccessModule =
        new DbUserAccessModule()
            .setAccessModule(rtTrainingModule)
            .setUser(user)
            .setBypassTime(rtTrainingBypassTime)
            .setCompletionTime(rtTrainingCompletionTime);
    AccessModuleStatus expectedRtTrainingModuleStatus =
        new AccessModuleStatus()
            .moduleName(AccessModule.COMPLIANCE_TRAINING)
            .completionEpochMillis(rtTrainingCompletionTime.getTime())
            .bypassEpochMillis(rtTrainingBypassTime.getTime());

    // Profile module: Completion time + expiryDays is 10 days before current time,
    // this module has expired for 10 days.
    Timestamp profileCompletionTime = Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    // It's bit wired that use the production code to extract the expiration time, but that is a
    // simple calculation, and even we wrote our own in test, it would just be the same code.
    Timestamp expectedProfileExpirationTime =
        deriveExpirationTimestamp(profileCompletionTime, expiryDays);
    DbUserAccessModule profileAccessModule =
        new DbUserAccessModule()
            .setAccessModule(profileConfirmModule)
            .setUser(user)
            .setCompletionTime(profileCompletionTime);
    AccessModuleStatus expectedProfileModuleStatus =
        new AccessModuleStatus()
            .moduleName(AccessModule.PROFILE_CONFIRMATION)
            .completionEpochMillis(profileCompletionTime.getTime())
            .expirationEpochMillis(expectedProfileExpirationTime.getTime());

    // Publication module: Completion time + expiryDays is 10 days after current time, this module
    // expired for 10 days.
    Timestamp publicationCompletionTime =
        Timestamp.from(now.minus(expiryDays - 10, ChronoUnit.DAYS));
    Timestamp expectedPublicationExpirationTime =
        deriveExpirationTimestamp(publicationCompletionTime, expiryDays);
    DbUserAccessModule publicationAccessModule =
        new DbUserAccessModule()
            .setAccessModule(publicationModule)
            .setUser(user)
            .setCompletionTime(publicationCompletionTime);
    AccessModuleStatus expectedPublicationModuleStatus =
        new AccessModuleStatus()
            .moduleName(AccessModule.PUBLICATION_CONFIRMATION)
            .completionEpochMillis(publicationCompletionTime.getTime())
            .expirationEpochMillis(expectedPublicationExpirationTime.getTime());

    // DUCC module: Completion time not present.
    DbUserAccessModule duccAccessModule =
        new DbUserAccessModule().setAccessModule(ducc).setUser(user);
    AccessModuleStatus expectedDuccModuleStatus =
        new AccessModuleStatus().moduleName(AccessModule.DATA_USER_CODE_OF_CONDUCT);

    userAccessModuleDao.saveAll(
        ImmutableList.of(
            twoFactorAuthUserAccessModule,
            duccAccessModule,
            rtTrainingAccessModule,
            publicationAccessModule,
            profileAccessModule));
    assertThat(accessModuleService.getAccessModuleStatus(user))
        .containsExactly(
            expected2FAModuleStatus,
            expectedDuccModuleStatus,
            expectedProfileModuleStatus,
            expectedPublicationModuleStatus,
            expectedRtTrainingModuleStatus);

    assertThat(
            accessModuleService.getAccessModuleStatus(
                user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT))
        .isEqualTo(Optional.of(expectedDuccModuleStatus));
  }

  @Test
  public void testGetClientAccessModuleStatus_moduleNotEnabledInEnv() {
    Instant now = Instant.ofEpochMilli(FakeClockConfiguration.NOW_TIME);
    long expiryDays = 365L;
    config.access.renewal.expiryDays = expiryDays;
    config.access.enableComplianceTraining = false;
    DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    DbAccessModule rtTrainingModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();

    // 2FA module: Module is not expirable, so no expiration date present.
    Timestamp twoFactorCompletionTime = Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    DbUserAccessModule twoFactorAuthUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setUser(user)
            .setCompletionTime(twoFactorCompletionTime);
    AccessModuleStatus expected2FAModuleStatus =
        new AccessModuleStatus()
            .moduleName(AccessModule.TWO_FACTOR_AUTH)
            .completionEpochMillis(twoFactorCompletionTime.getTime());

    Timestamp rtTrainingCompletionTime =
        Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    Timestamp rtTrainingBypassTime = Timestamp.from(now);
    DbUserAccessModule rtTrainingAccessModule =
        new DbUserAccessModule()
            .setAccessModule(rtTrainingModule)
            .setUser(user)
            .setBypassTime(rtTrainingBypassTime)
            .setCompletionTime(rtTrainingCompletionTime);
    userAccessModuleDao.saveAll(
        ImmutableList.of(twoFactorAuthUserAccessModule, rtTrainingAccessModule));
    assertThat(accessModuleService.getAccessModuleStatus(user))
        .containsExactly(expected2FAModuleStatus);

    assertThat(accessModuleService.getAccessModuleStatus(user, DbAccessModuleName.TWO_FACTOR_AUTH))
        .isEqualTo(Optional.of(expected2FAModuleStatus));
    assertThat(
            accessModuleService.getAccessModuleStatus(
                user, DbAccessModuleName.RT_COMPLIANCE_TRAINING))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void testModuleCompliant_bypassedAndExpired() {
    Instant now = Instant.ofEpochMilli(FakeClockConfiguration.NOW_TIME);
    long expiryDays = 365L;
    config.access.renewal.expiryDays = expiryDays;
    DbAccessModule duccModule =
        accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
    Timestamp completionTime = Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));
    Timestamp bypassTime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));

    DbUserAccessModule duccDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(duccModule)
            .setUser(user)
            .setCompletionTime(completionTime)
            .setBypassTime(bypassTime);
    userAccessModuleDao.save(duccDbUserAccessModule);

    assertThat(
            accessModuleService.isModuleCompliant(
                user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT))
        .isTrue();
  }

  @Test
  public void testModuleCompliant_expired() {
    Instant now = Instant.ofEpochMilli(FakeClockConfiguration.NOW_TIME);
    long expiryDays = 365L;
    config.access.renewal.expiryDays = expiryDays;
    DbAccessModule duccModule =
        accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
    DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(DbAccessModuleName.TWO_FACTOR_AUTH).get();
    Timestamp completionTime = Timestamp.from(now.minus(expiryDays + 10, ChronoUnit.DAYS));

    DbUserAccessModule duccDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(duccModule)
            .setUser(user)
            .setCompletionTime(completionTime);
    DbUserAccessModule twoFactorAuthDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setUser(user)
            .setCompletionTime(completionTime);
    userAccessModuleDao.saveAll(
        ImmutableList.of(twoFactorAuthDbUserAccessModule, duccDbUserAccessModule));

    // DUCC expired, but 2FA not because it is not expirable
    assertThat(
            accessModuleService.isModuleCompliant(
                user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT))
        .isFalse();
    assertThat(accessModuleService.isModuleCompliant(user, DbAccessModuleName.TWO_FACTOR_AUTH))
        .isTrue();
  }

  @Test
  public void testModuleCompliant_completeAndNotExpired() {
    Instant now = Instant.ofEpochMilli(FakeClockConfiguration.NOW_TIME);
    long expiryDays = 365L;
    config.access.renewal.expiryDays = expiryDays;

    DbAccessModule duccModule =
        accessModuleDao.findOneByName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
    Timestamp completionTime = Timestamp.from(now.minus(expiryDays - 10, ChronoUnit.DAYS));

    DbUserAccessModule existingDbUserAccessModule =
        new DbUserAccessModule()
            .setAccessModule(duccModule)
            .setUser(user)
            .setCompletionTime(completionTime);
    userAccessModuleDao.save(existingDbUserAccessModule);

    assertThat(
            accessModuleService.isModuleCompliant(
                user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT))
        .isTrue();
  }

  @Test
  public void testModuleCompliant_moduleNotEnabled() {
    config.access.enableComplianceTraining = false;
    DbAccessModule rtTrainingModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();

    DbUserAccessModule existingDbUserAccessModule =
        new DbUserAccessModule().setAccessModule(rtTrainingModule).setUser(user);
    userAccessModuleDao.save(existingDbUserAccessModule);
    assertThat(
            accessModuleService.isModuleCompliant(user, DbAccessModuleName.RT_COMPLIANCE_TRAINING))
        .isTrue();
  }

  @Test
  public void testModuleCompliant_moduleNotEnabled_notExistInDb() {
    config.access.enableComplianceTraining = false;
    assertThat(
            accessModuleService.isModuleCompliant(user, DbAccessModuleName.RT_COMPLIANCE_TRAINING))
        .isTrue();
  }

  @Test
  public void testModuleCompliant_moduleNotExist() {
    assertThat(
            accessModuleService.isModuleCompliant(
                user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT))
        .isFalse();
  }

  private static Optional<Instant> nullableTimestampToOptionalInstant(
      @Nullable Timestamp complianceTrainingBypassTime) {
    return Optional.ofNullable(complianceTrainingBypassTime).map(Timestamp::toInstant);
  }
}
