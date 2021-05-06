package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.UserDao.DbAdminTableUser;
import org.pmiops.workbench.db.dao.UserDao.UserCountByDisabledAndAccessTiers;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
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
  @Autowired private InstitutionDao institutionDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  @Autowired private UserDao userDao;

  @Before
  public void setup() {
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
  }

  @Test
  public void testGetUserCountGaugeData_singleValue() {
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
    assertThat(split(row.getAccessTierShortNames())).contains("unregistered");
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
    institution.setDuaTypeEnum(DuaType.MASTER);
    return institutionDao.save(institution);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }
}
