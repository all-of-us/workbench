package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbOneTimeCode;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class OneTimeCodeDaoTest {
  @Autowired private UserDao userDao;
  @Autowired private OneTimeCodeDao oneTimeCodeDao;

  private static final Timestamp CURRENT_TIMESTAMP = Timestamp.from(Instant.now());
  private DbUser user;

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());
  }

  private DbOneTimeCode createValidDbOneTimeCode() {
    return oneTimeCodeDao.save(new DbOneTimeCode().setUser(user).setUsedTime(CURRENT_TIMESTAMP));
  }

  @Test
  public void testCRUD() {
    DbOneTimeCode oneTimeCode = createValidDbOneTimeCode();

    assertThat(oneTimeCodeDao.findById(oneTimeCode.getId()).get()).isEqualTo(oneTimeCode);

    Timestamp tomorrow = Timestamp.from(CURRENT_TIMESTAMP.toInstant().plus(1, ChronoUnit.DAYS));
    oneTimeCodeDao.save(oneTimeCode.setUsedTime(tomorrow));
    assertThat(Iterables.size(oneTimeCodeDao.findAll())).isEqualTo(1);
    assertThat(oneTimeCodeDao.findById(oneTimeCode.getId()).get().getUsedTime())
        .isEqualTo(tomorrow);

    oneTimeCodeDao.delete(oneTimeCode);
    assertThat(oneTimeCodeDao.findById(oneTimeCode.getId()).isPresent()).isFalse();
  }

  @Test
  public void testFindByStringId_findsCode() {
    DbOneTimeCode oneTimeCode = createValidDbOneTimeCode();
    assertThat(oneTimeCodeDao.findByStringId(oneTimeCode.getId().toString()).get())
        .isEqualTo(oneTimeCode);
  }

  @Test
  public void testFindByStringId_returnsEmptyForInvalidUUID() {
    assertThat(oneTimeCodeDao.findByStringId("not a uuid").isPresent()).isFalse();
  }
}
