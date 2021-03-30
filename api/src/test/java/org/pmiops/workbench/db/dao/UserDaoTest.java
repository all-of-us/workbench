package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.UserDao.UserCountGaugeLabelsAndValue;
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

    List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    assertThat(rows).hasSize(1);
    final UserCountGaugeLabelsAndValue row = rows.get(0);
    assertThat(row.getAccessTierShortNames()).contains(registeredTier.getShortName());
    assertThat(row.getDisabled()).isFalse();
    assertThat(row.getUserCount()).isEqualTo(1L);
  }

  @Test
  public void testGetUserCountGaugeData_noUsers() {
    List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    assertThat(rows).isEmpty();
  }

  @Test
  public void testGetUserCountGaugeData_multipleUsers() {
    final DbInstitution institution = createInstitution();

    insertTestUsers(false, 2, institution, registeredTier);
    insertTestUsers(false, 1, institution, registeredTier);
    insertTestUsers(true, 5, institution, registeredTier);
    insertTestUsers(false, 10, institution);

    final List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    // registered/enabled, registered/disabled, and unregistered/enabled
    assertThat(rows).hasSize(3);

    // registered/enabled: 3
    assertThat(
            rows.stream()
                .filter(
                    r ->
                        r.getAccessTierShortNames() != null
                            && r.getAccessTierShortNames().contains(registeredTier.getShortName()))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountGaugeLabelsAndValue::getUserCount)
                .orElse(-1L))
        .isEqualTo(3);

    // registered/disabled: 5
    assertThat(
            rows.stream()
                .filter(
                    r ->
                        r.getAccessTierShortNames() != null
                            && r.getAccessTierShortNames().contains(registeredTier.getShortName()))
                .filter(UserCountGaugeLabelsAndValue::getDisabled)
                .findFirst()
                .map(UserCountGaugeLabelsAndValue::getUserCount)
                .orElse(-1L))
        .isEqualTo(5);

    // unregistered/enabled: 10
    assertThat(
            rows.stream()
                .filter(
                    r ->
                        r.getAccessTierShortNames() == null
                            || !r.getAccessTierShortNames().contains(registeredTier.getShortName()))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountGaugeLabelsAndValue::getUserCount)
                .orElse(-1L))
        .isEqualTo(10);
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

  private DbUserAccessTier addUserToTier(DbUser user, DbAccessTier tier) {
    final DbUserAccessTier entryToInsert =
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(tier)
            .setTierAccessStatus(TierAccessStatus.ENABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now());
    return userAccessTierDao.save(entryToInsert);
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
