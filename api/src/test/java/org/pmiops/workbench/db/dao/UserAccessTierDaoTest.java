package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class UserAccessTierDaoTest {
  @Autowired private UserDao userDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private UserAccessTierDao userAccessTierDao;

  DbUser user;
  DbAccessTier registeredTier;
  DbAccessTier controlledTier;

  @Before
  public void setup() {
    user = new DbUser();
    user.setUserId(100);
    user = userDao.save(user);

    registeredTier = TestMockFactory.createDefaultAccessTier(accessTierDao);

    controlledTier =
        accessTierDao.save(
            new DbAccessTier()
                .setAccessTierId(2)
                .setShortName("controlled")
                .setDisplayName("Controlled Tier")
                .setAuthDomainName("Controlled Tier Auth Domain")
                .setAuthDomainGroupEmail("ct-users@fake-research-aou.org")
                .setServicePerimeter("controlled/tier/perimeter"));
  }

  @Test
  public void test_getByUserAndAccessTier_empty() {
    assertThat(userAccessTierDao.getByUserAndAccessTier(user, registeredTier)).isEmpty();
  }

  @Test
  public void test_getByUserAndAccessTier_RT() {
    final Instant testStart = Instant.now();

    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setAccessStatus(TierAccessStatus.ENABLED));

    Optional<DbUserAccessTier> entryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(entryMaybe).isPresent();
    DbUserAccessTier entry = entryMaybe.get();
    assertThat(entry.getUser()).isEqualTo(user);
    assertThat(entry.getAccessTier()).isEqualTo(registeredTier);
    assertThat(entry.getAccessStatus()).isEqualTo(TierAccessStatus.ENABLED);
    assertThat(entry.getFirstEnabled().toInstant().isAfter(testStart)).isTrue();
    assertThat(entry.getLastUpdated().toInstant().isAfter(testStart)).isTrue();
  }

  @Test
  public void test_getByUserAndAccessTier_RT_disabled() {
    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setAccessStatus(TierAccessStatus.DISABLED));

    Optional<DbUserAccessTier> entryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(entryMaybe).isPresent();
    DbUserAccessTier entry = entryMaybe.get();
    assertThat(entry.getAccessStatus()).isEqualTo(TierAccessStatus.DISABLED);
  }

  @Test
  public void test_getByUserAndAccessTier_CT_no_access() {
    userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(registeredTier)
            .setAccessStatus(TierAccessStatus.ENABLED));

    assertThat(userAccessTierDao.getByUserAndAccessTier(user, controlledTier)).isEmpty();
  }
}
