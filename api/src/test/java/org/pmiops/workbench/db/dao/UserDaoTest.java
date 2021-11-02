package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.jdo.annotations.Transactional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.UserDao.DbAdminTableUser;
import org.pmiops.workbench.db.dao.UserDao.UserCountByDisabledAndAccessTiers;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserDaoTest extends SpringTest {
  private static final String STREET_ADDRESS_1 = "101 Main St";
  private static final String STREET_ADDRESS_2 = "# 202";
  private static final String CITY = "New Braunfels";
  private static final String STATE = "TX";
  private static final String COUNTRY = "US";

  private DbAccessTier registeredTier;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private InstitutionDao institutionDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  @Autowired private UserDao userDao;

  @BeforeEach
  public void setup() {
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    TestMockFactory.createAccessModules(accessModuleDao);
  }

  @Test
  public void testGetUserCountGaugeData_singleValue() throws Exception {
    DbUser user1 = new DbUser();
    user1.setDisabled(false);
    user1 = userDao.save(user1);
    addUserToTier(user1, registeredTier);

    List<UserCountByDisabledAndAccessTiers> rows = userDao.getUserCountGaugeData();
    assertThat(rows).hasSize(1);
    final UserCountByDisabledAndAccessTiers row = rows.get(0);
    assertThat(row.getAccessTierShortNames()).isNotNull();
    assertThat(split(row.getAccessTierShortNames())).contains(registeredTier.getShortName());
    assertThat(row.getDisabled()).isFalse();
    assertThat(row.getUserCount()).isEqualTo(1L);

    // Iterate all getter methods and make sure all return value is non-null.
    Class<UserCountByDisabledAndAccessTiers> projectionClass =
        UserCountByDisabledAndAccessTiers.class;
    for (Method method : projectionClass.getMethods()) {
      if (method.getName().startsWith("get")) {
        assertThat(method.invoke(rows.get(0))).isNotNull();
      }
    }
  }

  @Test
  public void testGetUserCountGaugeData_disabledTier() {
    DbUser user1 = new DbUser();
    user1.setDisabled(false);
    user1 = userDao.save(user1);
    addUserToTier(user1, registeredTier, TierAccessStatus.DISABLED);
    List<UserCountByDisabledAndAccessTiers> rows = userDao.getUserCountGaugeData();
    assertThat(rows).hasSize(1);
    final UserCountByDisabledAndAccessTiers row = rows.get(0);
    assertThat(split(row.getAccessTierShortNames())).contains("[unregistered]");
    assertThat(row.getDisabled()).isFalse();
    assertThat(row.getUserCount()).isEqualTo(1L);
  }

  @Test
  public void testGetUserCountGaugeData_noUsers() {
    List<UserCountByDisabledAndAccessTiers> rows = userDao.getUserCountGaugeData();
    assertThat(rows).isEmpty();
  }

  @Test
  public void testGetUserCountGaugeData_multipleUsers() {
    final DbInstitution institution = createInstitution();

    insertTestUsers(false, 2, institution, registeredTier);
    insertTestUsers(false, 1, institution, registeredTier);
    insertTestUsers(true, 5, institution, registeredTier);
    insertTestUsers(false, 10, institution);

    final List<UserCountByDisabledAndAccessTiers> rows = userDao.getUserCountGaugeData();
    // registered/enabled, registered/disabled, and unregistered/enabled
    assertThat(rows).hasSize(3);

    // registered/enabled: 3
    assertThat(
            rows.stream()
                .filter(
                    r -> split(r.getAccessTierShortNames()).contains(registeredTier.getShortName()))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountByDisabledAndAccessTiers::getUserCount)
                .orElse(-1L))
        .isEqualTo(3);

    // registered/disabled: 5
    assertThat(
            rows.stream()
                .filter(
                    r -> split(r.getAccessTierShortNames()).contains(registeredTier.getShortName()))
                .filter(UserCountByDisabledAndAccessTiers::getDisabled)
                .findFirst()
                .map(UserCountByDisabledAndAccessTiers::getUserCount)
                .orElse(-1L))
        .isEqualTo(5);

    // unregistered/enabled: 10
    assertThat(
            rows.stream()
                .filter(r -> split(r.getAccessTierShortNames()).contains("[unregistered]"))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountByDisabledAndAccessTiers::getUserCount)
                .orElse(-1L))
        .isEqualTo(10);
  }

  private List<String> split(String input) {
    return Splitter.on(',').splitToList(input);
  }

  @Test
  public void testGetAdminTableUsers() throws Exception {
    // Make sure AdminTable projection works.
    Timestamp nowTime = now();
    String contactEmail = "1@foo.com";
    DbUser user = new DbUser();
    user.setUsername("name");
    user.setDisabled(true);
    user.setContactEmail(contactEmail);
    user.setGivenName("givenName");
    user.setFamilyName("familyName");
    user.setCreationTime(nowTime);
    user.setFirstSignInTime(nowTime);

    final DbInstitution institution = createInstitution();
    user = userDao.save(user);
    createAffiliation(user, institution);
    addUserToTier(user, registeredTier);

    final DbAccessModule twoFactorAuthModule =
        accessModuleDao.findOneByName(AccessModuleName.TWO_FACTOR_AUTH).get();
    final DbAccessModule rtTrainingModule =
        accessModuleDao.findOneByName(AccessModuleName.RT_COMPLIANCE_TRAINING).get();
    final DbAccessModule ctTrainingModule =
        accessModuleDao.findOneByName(AccessModuleName.CT_COMPLIANCE_TRAINING).get();
    final DbAccessModule eRACommonsModule =
        accessModuleDao.findOneByName(AccessModuleName.ERA_COMMONS).get();
    final DbAccessModule duccModule =
        accessModuleDao.findOneByName(AccessModuleName.DATA_USER_CODE_OF_CONDUCT).get();
    final DbAccessModule rasConfirmModule =
        accessModuleDao.findOneByName(AccessModuleName.RAS_LOGIN_GOV).get();
    Instant now = Instant.now();
    Timestamp twoFactorAuthBypassTime = Timestamp.from(now.minusSeconds(10));
    Timestamp twoFactorAuthCompleteTime = Timestamp.from(now.minusSeconds(20));
    Timestamp rtTrainingBypassTime = Timestamp.from(now.minusSeconds(30));
    Timestamp rtTrainingCompleteTime = Timestamp.from(now.minusSeconds(40));
    Timestamp eRABypassTime = Timestamp.from(now.minusSeconds(50));
    Timestamp eRACompleteTime = Timestamp.from(now.minusSeconds(60));
    Timestamp duccBypassTime = Timestamp.from(now.minusSeconds(70));
    Timestamp duccCompleteTime = Timestamp.from(now.minusSeconds(80));
    Timestamp rasBypassTime = Timestamp.from(now.minusSeconds(90));
    Timestamp ctTrainingBypassTime = Timestamp.from(now.minusSeconds(100));
    Timestamp ctTrainingCompleteTime = Timestamp.from(now.minusSeconds(110));
    Timestamp rasCompleteTime = Timestamp.from(now);
    addUserAccessModule(
        user, twoFactorAuthModule, twoFactorAuthBypassTime, twoFactorAuthCompleteTime);
    addUserAccessModule(user, rtTrainingModule, rtTrainingBypassTime, rtTrainingCompleteTime);
    addUserAccessModule(user, ctTrainingModule, ctTrainingBypassTime, ctTrainingCompleteTime);
    addUserAccessModule(user, eRACommonsModule, eRABypassTime, eRACompleteTime);
    addUserAccessModule(user, duccModule, duccBypassTime, duccCompleteTime);
    addUserAccessModule(user, rasConfirmModule, rasBypassTime, rasCompleteTime);
    List<DbAdminTableUser> rows = userDao.getAdminTableUsers();
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getEraCommonsBypassTime()).isEqualTo(eRABypassTime);
    assertThat(rows.get(0).getEraCommonsCompletionTime()).isEqualTo(eRACompleteTime);
    assertThat(rows.get(0).getComplianceTrainingBypassTime()).isEqualTo(rtTrainingBypassTime);
    assertThat(rows.get(0).getComplianceTrainingCompletionTime()).isEqualTo(rtTrainingCompleteTime);
    assertThat(rows.get(0).getCtComplianceTrainingBypassTime()).isEqualTo(ctTrainingBypassTime);
    assertThat(rows.get(0).getCtComplianceTrainingCompletionTime())
        .isEqualTo(ctTrainingCompleteTime);
    assertThat(rows.get(0).getDataUseAgreementBypassTime()).isEqualTo(duccBypassTime);
    assertThat(rows.get(0).getDataUseAgreementCompletionTime()).isEqualTo(duccCompleteTime);
    assertThat(rows.get(0).getTwoFactorAuthBypassTime()).isEqualTo(twoFactorAuthBypassTime);
    assertThat(rows.get(0).getTwoFactorAuthCompletionTime()).isEqualTo(twoFactorAuthCompleteTime);
    assertThat(rows.get(0).getRasLinkLoginGovBypassTime()).isEqualTo(rasBypassTime);
    assertThat(rows.get(0).getRasLinkLoginGovCompletionTime()).isEqualTo(rasCompleteTime);

    final DbAdminTableUser row = rows.get(0);
    Class<DbAdminTableUser> dbAdminUserClass = DbAdminTableUser.class;
    // Iterate all getter methods and make sure all return value is non-null.
    for (Method method : dbAdminUserClass.getMethods()) {
      if (method.getName().startsWith("get")) {
        assertThat(method.invoke(row)).isNotNull();
      }
    }
  }

  @Test
  public void test_getAdminTableUsers_disabledTier() {
    DbUser user1 = new DbUser();
    user1.setDisabled(false);
    user1 = userDao.save(user1);
    addUserToTier(user1, registeredTier, TierAccessStatus.DISABLED);

    List<DbAdminTableUser> rows = userDao.getAdminTableUsers();
    assertThat(rows).hasSize(1);
    final DbAdminTableUser row = rows.get(0);
    assertThat(row.getAccessTierShortNames()).isNull();
    assertThat(row.getDisabled()).isFalse();
  }

  @Test
  public void test_findUsersBySearchStringAndTier_empty() {
    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    assertThat(userDao.findUsersBySearchStringAndTier("any", ascendingByUsername, "any")).isEmpty();
  }

  @Test
  public void test_findUsersBySearchStringAndTier_givenName() {
    DbUser user = new DbUser();
    user.setGivenName("Alice");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "A", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);

    result =
        userDao.findUsersBySearchStringAndTier(
            "lice", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);
  }

  @Test
  public void test_findUsersBySearchStringAndTier_familyName() {
    DbUser user = new DbUser();
    user.setFamilyName("Lee");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "Le", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);

    result =
        userDao.findUsersBySearchStringAndTier(
            "ee", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);
  }

  @Test
  public void test_findUsersBySearchStringAndTier_username() {
    DbUser user = new DbUser();
    user.setUsername("scienceGuy");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "sci", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);

    result =
        userDao.findUsersBySearchStringAndTier(
            "Guy", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(user);
  }

  @Test
  public void test_findUsersBySearchStringAndTier_wrongTier() {
    DbUser user = new DbUser();
    user.setGivenName("Alice");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    // this also won't match
    TestMockFactory.createControlledTierForTests(accessTierDao);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier("A", ascendingByUsername, "wrong-tier");
    assertThat(result).isEmpty();
  }

  @Test
  public void test_findUsersBySearchStringAndTier_controlled_match() {
    DbUser user = new DbUser();
    user.setGivenName("Alice");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    addUserToTier(user, controlledTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "A", ascendingByUsername, controlledTier.getShortName());
    assertThat(result).containsExactly(user);
  }

  // RW-7533 regression test: confirm that a user does not appear in CT search results if they
  // previously had Controlled Tier access but now do not

  @Test
  public void test_findUsersBySearchStringAndTier_controlled_disabled() {
    DbUser user = new DbUser();
    user.setGivenName("Alice");
    user = userDao.save(user);
    addUserToTier(user, registeredTier);

    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    addUserToTier(user, controlledTier);
    removeUserFromTier(user, controlledTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "A", ascendingByUsername, controlledTier.getShortName());
    assertThat(result).isEmpty();
  }

  @Test
  public void test_findUsersBySearchStringAndTier_multi() {
    DbUser alice = new DbUser();
    alice.setGivenName("Alice");
    alice.setFamilyName("Funk");
    alice.setFamilyName("afunk123");
    alice = userDao.save(alice);
    addUserToTier(alice, registeredTier);

    DbUser bob = new DbUser();
    bob.setGivenName("Bob");
    bob.setFamilyName("O'Brien");
    bob.setUsername("bobo1");
    bob = userDao.save(bob);
    addUserToTier(bob, registeredTier);

    DbUser taylor = new DbUser();
    taylor.setGivenName("Taylor");
    taylor.setFamilyName("Nakamura");
    taylor.setUsername("captain");
    taylor = userDao.save(taylor);
    addUserToTier(taylor, registeredTier);

    final Sort ascendingByUsername = Sort.by(Sort.Direction.ASC, "username");

    // 'a' matches 'afunk123' and all of Taylor's fields
    List<DbUser> result =
        userDao.findUsersBySearchStringAndTier(
            "a", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(alice, taylor).inOrder();

    // 'I' matches 'Alice', `O'Brien`, and 'captain' because it's case-insensitive
    result =
        userDao.findUsersBySearchStringAndTier(
            "I", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).containsExactly(alice, bob, taylor).inOrder();

    result =
        userDao.findUsersBySearchStringAndTier(
            "q", ascendingByUsername, registeredTier.getShortName());
    assertThat(result).isEmpty();
  }

  private List<DbUser> insertTestUsers(
      boolean isDisabled, long numUsers, DbInstitution institution, DbAccessTier... tiers) {

    ImmutableList.Builder<DbUser> resultList = ImmutableList.builder();

    for (int i = 0; i < numUsers; ++i) {
      DbUser user = new DbUser();
      user.setGivenName("Bar");
      user.setFamilyName("Foo");
      user.setUsername("jaycarlton@aou.biz");
      final DbAddress address = createAddress();
      address.setUser(user);
      user.setAddress(address); // ?
      user.setDisabled(isDisabled);
      user = userDao.save(user);

      createAffiliation(user, institution);
      for (DbAccessTier tier : tiers) {
        addUserToTier(user, tier);
      }

      resultList.add(userDao.save(user));
    }
    return resultList.build();
  }

  private DbUserAccessTier addUserToTier(DbUser user, DbAccessTier tier, TierAccessStatus status) {
    final DbUserAccessTier entryToInsert =
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(tier)
            .setTierAccessStatus(status)
            .setFirstEnabled(now())
            .setLastUpdated(now());
    return userAccessTierDao.save(entryToInsert);
  }

  private DbUserAccessTier addUserToTier(DbUser user, DbAccessTier tier) {
    return addUserToTier(user, tier, TierAccessStatus.ENABLED);
  }

  private void removeUserFromTier(DbUser user, DbAccessTier tier) {
    // if not present, do nothing
    userAccessTierDao
        .getByUserAndAccessTier(user, tier)
        .ifPresent(
            uat -> userAccessTierDao.save(uat.setTierAccessStatus(TierAccessStatus.DISABLED)));
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

  @NotNull
  private DbAddress createAddress() {
    final DbAddress address = new DbAddress();
    address.setStreetAddress1(STREET_ADDRESS_1);
    address.setStreetAddress2(STREET_ADDRESS_2);
    address.setZipCode("78130");
    address.setCity(CITY);
    address.setState(STATE);
    address.setCountry(COUNTRY);
    return address;
  }

  private DbVerifiedInstitutionalAffiliation createAffiliation(
      DbUser user, DbInstitution institution) {
    final DbVerifiedInstitutionalAffiliation affiliation = new DbVerifiedInstitutionalAffiliation();
    affiliation.setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    affiliation.setUser(user);
    affiliation.setInstitution(institution);
    userDao.save(user);
    return verifiedInstitutionalAffiliationDao.save(affiliation);
  }

  private DbInstitution createInstitution() {
    final DbInstitution institution = new DbInstitution();
    institution.setShortName("aa");
    institution.setDisplayName("MIT");
    institution.setOrganizationTypeEnum(OrganizationType.EDUCATIONAL_INSTITUTION);
    return institutionDao.save(institution);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }
}
