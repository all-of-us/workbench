package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserAccessTierDaoTest extends SpringTest {
  @Autowired private UserDao userDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private UserAccessTierDao userAccessTierDao;

  private DbUser user;
  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  @Before
  public void setup() {
    user = new DbUser();
    user.setUserId(100);
    user = userDao.save(user);

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
  }

  @Test
  public void test_getByUserAndAccessTier_empty() {
    assertThat(userAccessTierDao.getByUserAndAccessTier(user, registeredTier)).isEmpty();
  }

  @Test
  public void test_getByUserAndAccessTier_RT() {
    // arbitrary nonzero time amount
    final Instant recently = Instant.now().minusSeconds(10);

    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setTierAccessStatus(TierAccessStatus.ENABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    Optional<DbUserAccessTier> entryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(entryMaybe).isPresent();
    DbUserAccessTier entry = entryMaybe.get();
    assertThat(entry.getUser()).isEqualTo(user);
    assertThat(entry.getAccessTier()).isEqualTo(registeredTier);
    assertThat(entry.getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    assertThat(entry.getFirstEnabled().toInstant().isAfter(recently)).isTrue();
    assertThat(entry.getLastUpdated().toInstant().isAfter(recently)).isTrue();
  }

  @Test
  public void test_getByUserAndAccessTier_RT_disabled() {
    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setTierAccessStatus(TierAccessStatus.DISABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    Optional<DbUserAccessTier> entryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(entryMaybe).isPresent();
    DbUserAccessTier entry = entryMaybe.get();
    assertThat(entry.getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.DISABLED);
  }

  @Test
  public void test_getByUserAndAccessTier_CT_no_access() {
    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setTierAccessStatus(TierAccessStatus.ENABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    assertThat(userAccessTierDao.getByUserAndAccessTier(user, controlledTier)).isEmpty();
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }
}
