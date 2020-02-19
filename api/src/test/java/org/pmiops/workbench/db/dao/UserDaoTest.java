package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao.UserCountGaugeLabelsAndValue;
import org.pmiops.workbench.db.model.CommonStorageEnums;
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

  @Before
  public void setUp() {

  }

  @Test
  public void testCountByAttributes() {
    DbUser user1 = new DbUser();
    user1.setDisabled(false);
    user1.setBetaAccessBypassTime(NOW);
    final Short dataAccessLevelStorage = CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED);
    user1.setDataAccessLevel(dataAccessLevelStorage);
    user1 = userDao.save(user1);

    List<UserCountGaugeLabelsAndValue> rows = userDao.getDataAccessLevelDisabledAndBetaBypassedToCount();
    assertThat(rows).hasSize(1);
    final UserCountGaugeLabelsAndValue row = rows.get(0);
    assertThat(row.getDataAccessLevel()).isEqualTo(dataAccessLevelStorage);
    assertThat(row.getBetaIsBypassed()).isTrue();
    assertThat(row.getDisabled()).isFalse();
    assertThat(row.getUserCount()).isEqualTo(1L);
  }
}
