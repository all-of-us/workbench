package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao.UserCountGaugeLabelsAndValue;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserDaoTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final Timestamp BETA_ACCESS_REQUEST_TIME =
      Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final String STREET_ADDRESS_1 = "101 Main St";
  private static final String STREET_ADDRESS_2 = "# 202";
  private static final String CITY = "New Braunfels";
  private static final String STATE = "TX";
  private static final String COUNTRY = "US";

  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  @Autowired private InstitutionDao institutionDao;
  @Autowired private UserDao userDao;

  @Test
  public void testgetUserCountGaugeData_singleValue() {
    DbUser user1 = new DbUser();
    user1.setDisabled(false);
    user1.setBetaAccessBypassTime(NOW);
    final Short dataAccessLevelStorage =
        DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED);
    user1.setDataAccessLevel(dataAccessLevelStorage);
    user1 = userDao.save(user1);

    List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    assertThat(rows).hasSize(1);
    final UserCountGaugeLabelsAndValue row = rows.get(0);
    assertThat(row.getDataAccessLevel()).isEqualTo(dataAccessLevelStorage);
    assertThat(row.getBetaIsBypassed()).isTrue();
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
    insertMultipleUsers();

    final List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    assertThat(rows).hasSize(4);

    assertThat(
            rows.stream()
                .filter(UserCountGaugeLabelsAndValue::getBetaIsBypassed)
                .anyMatch(UserCountGaugeLabelsAndValue::getDisabled))
        .isFalse();

    assertThat(
            rows.stream()
                .filter(r -> !r.getBetaIsBypassed())
                .filter(
                    r ->
                        r.getDataAccessLevel()
                            .equals(
                                DbStorageEnums.dataAccessLevelToStorage(
                                    DataAccessLevel.REGISTERED)))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountGaugeLabelsAndValue::getUserCount)
                .orElse(-1L))
        .isEqualTo(1);

    assertThat(
            rows.stream()
                .filter(UserCountGaugeLabelsAndValue::getBetaIsBypassed)
                .filter(
                    r ->
                        r.getDataAccessLevel()
                            .equals(
                                DbStorageEnums.dataAccessLevelToStorage(
                                    DataAccessLevel.UNREGISTERED)))
                .filter(r -> !r.getDisabled())
                .findFirst()
                .map(UserCountGaugeLabelsAndValue::getUserCount)
                .orElse(-1L))
        .isEqualTo(10);
  }

  public void insertMultipleUsers() {
    final DbInstitution institution = createInstitution();
    insertTestUsers(false, DataAccessLevel.PROTECTED, true, 2, institution);
    insertTestUsers(false, DataAccessLevel.REGISTERED, false, 1, institution);
    insertTestUsers(true, DataAccessLevel.PROTECTED, false, 5, institution);
    insertTestUsers(false, DataAccessLevel.UNREGISTERED, true, 10, institution);
  }

  private List<DbUser> insertTestUsers(
      boolean isDisabled,
      DataAccessLevel dataAccessLevel,
      boolean isBetaBypassed,
      long numUsers,
      DbInstitution institution) {
    ImmutableList.Builder<DbUser> resultList = ImmutableList.builder();

    for (int i = 0; i < numUsers; ++i) {
      DbUser user = new DbUser();
      user.setGivenName("Bar");
      user.setFamilyName("Foo");
      user.setUsername("jaycarlton@aou.biz");
      user.setBetaAccessRequestTime(BETA_ACCESS_REQUEST_TIME);
      final DbAddress address = createAddress();
      address.setUser(user);
      user.setAddress(address); // ?
      user.setDisabled(isDisabled);
      if (isBetaBypassed) {
        user.setBetaAccessBypassTime(NOW);
      }
      user.setDataAccessLevel(DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel));
      createAffiliation(user, institution);
      resultList.add(userDao.save(user));
    }
    return resultList.build();
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
}
