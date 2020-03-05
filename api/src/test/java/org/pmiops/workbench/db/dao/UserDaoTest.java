package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao.UserCountGaugeLabelsAndValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserDaoTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));

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
    insertTestUsers(false, DataAccessLevel.PROTECTED, true, 2);
    insertTestUsers(false, DataAccessLevel.REGISTERED, false, 1);
    insertTestUsers(true, DataAccessLevel.PROTECTED, false, 5);
    insertTestUsers(false, DataAccessLevel.UNREGISTERED, true, 10);

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

  private List<DbUser> insertTestUsers(
      boolean isDisabled, DataAccessLevel dataAccessLevel, boolean isBetaBypassed, long numUsers) {
    ImmutableList.Builder<DbUser> resultList = ImmutableList.builder();
    for (int i = 0; i < numUsers; ++i) {
      DbUser user = new DbUser();
      user.setDisabled(isDisabled);
      if (isBetaBypassed) {
        user.setBetaAccessBypassTime(NOW);
      }
      user.setDataAccessLevel(DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel));
      resultList.add(userDao.save(user));
    }
    return resultList.build();
  }
}
