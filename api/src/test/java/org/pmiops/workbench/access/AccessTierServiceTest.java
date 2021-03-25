package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class AccessTierServiceTest {
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessTierService accessTierService;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserDao userDao;

  private static DbUser user;
  private static WorkbenchConfig config;

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());

  @Import({
    AccessTierServiceImpl.class,
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DbUser user() {
      return user;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig config() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setup() {
    user = new DbUser();
    user.setUsername("user");
    user = userDao.save(user);

    config = WorkbenchConfig.createEmptyConfig();
  }

  @Test
  public void test_getAllTiers_empty() {
    assertThat(accessTierService.getAllTiers()).isEmpty();
  }

  @Test
  public void test_getAllTiers_2() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    assertThat(accessTierService.getAllTiers()).containsExactly(registeredTier, controlledTier);
  }

  @Test(expected = ServerErrorException.class)
  public void test_getRegisteredTier_empty() {
    accessTierService.getRegisteredTier();
  }

  @Test
  public void test_getRegisteredTier_registered() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    assertThat(accessTierService.getRegisteredTier()).isEqualTo(registeredTier);
  }

  @Test(expected = ServerErrorException.class)
  public void test_getRegisteredTier_missing() {
    // wrong tier
    TestMockFactory.createControlledTierForTests(accessTierDao);
    accessTierService.getRegisteredTier();
  }

  @Test
  public void test_getAccessTiersForUser_empty() {
    assertThat(accessTierService.getAccessTiersForUser(user)).isEmpty();
  }

  @Test
  public void test_getAccessTiersForUser_unregistered() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    assertThat(accessTierService.getAccessTiersForUser(user)).isEmpty();
  }

  @Test
  public void test_getAccessTiersForUser_registered() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    addDaoEntry(user, registeredTier, TierAccessStatus.ENABLED);
    assertThat(accessTierService.getAccessTiersForUser(user)).containsExactly(registeredTier);
  }

  @Test
  public void test_getAccessTiersForUser_registered_disabled() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    addDaoEntry(user, registeredTier, TierAccessStatus.DISABLED);
    assertThat(accessTierService.getAccessTiersForUser(user)).isEmpty();
  }

  @Test
  public void test_getAccessTiersForUser_registered_controlled() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    addDaoEntry(user, registeredTier, TierAccessStatus.ENABLED);

    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    addDaoEntry(user, controlledTier, TierAccessStatus.ENABLED);

    assertThat(accessTierService.getAccessTiersForUser(user))
        .containsExactly(registeredTier, controlledTier);
  }

  @Test
  public void test_addUserToRegisteredTier_new() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    // simply to show a non-Registered tier exists but we don't add the user to it
    TestMockFactory.createControlledTierForTests(accessTierDao);

    accessTierService.addUserToRegisteredTier(user);

    Iterable<DbUserAccessTier> userAccessTiers = userAccessTierDao.findAll();
    assertThat(userAccessTiers).hasSize(1);

    Optional<DbUserAccessTier> uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();

    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
  }

  @Test
  public void test_addUserToRegisteredTier_idempotent() {
    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    accessTierService.addUserToRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    Optional<DbUserAccessTier> uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    final Timestamp firstEnabled = uat.get().getFirstEnabled();
    final Timestamp lastUpdated = uat.get().getLastUpdated();

    // wait a second
    CLOCK.increment(1000);

    accessTierService.addUserToRegisteredTier(user);

    // no change

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    assertThat(uat.get().getFirstEnabled()).isEqualTo(firstEnabled);
    assertThat(uat.get().getLastUpdated()).isEqualTo(lastUpdated);
  }

  @Test
  public void test_removeUserFromRegisteredTier_new() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    // does nothing
    accessTierService.removeUserFromRegisteredTier(user);
    assertThat(userAccessTierDao.findAll()).isEmpty();
  }

  @Test
  public void test_removeUserFromRegisteredTier_existing() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    // adds a DB entry for (user, registered)
    accessTierService.addUserToRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    Optional<DbUserAccessTier> uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    final Timestamp firstEnabled = uat.get().getFirstEnabled();
    final Timestamp lastUpdated = uat.get().getLastUpdated();

    // wait a second
    CLOCK.increment(1000);

    // updates the DB entry by setting it to DISABLED
    accessTierService.removeUserFromRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.DISABLED);
    assertThat(uat.get().getFirstEnabled()).isEqualTo(firstEnabled);
    assertThat(uat.get().getLastUpdated()).isGreaterThan(lastUpdated);
  }

  @Test
  public void test_removeUserFromRegisteredTier_existing_idempotent() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    // adds a DB entry for (user, registered)
    accessTierService.addUserToRegisteredTier(user);

    // updates the DB entry by setting it to DISABLED
    accessTierService.removeUserFromRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    Optional<DbUserAccessTier> uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    final Timestamp lastUpdated = uat.get().getLastUpdated();

    // wait a second
    CLOCK.increment(1000);

    accessTierService.removeUserFromRegisteredTier(user);

    // no change

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getLastUpdated()).isEqualTo(lastUpdated);
  }

  @Test
  public void test_add_remove_add() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    // adds a DB entry for (user, registered)
    accessTierService.addUserToRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    Optional<DbUserAccessTier> uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    final Timestamp firstEnabled = uat.get().getFirstEnabled();
    assertThat(uat.get().getLastUpdated()).isEqualTo(firstEnabled);

    // wait a second
    CLOCK.increment(1000);

    // updates the DB entry by setting it to DISABLED
    accessTierService.removeUserFromRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.DISABLED);
    assertThat(uat.get().getFirstEnabled()).isEqualTo(firstEnabled);

    final Timestamp disabledTime = uat.get().getLastUpdated();
    assertThat(disabledTime).isGreaterThan(firstEnabled);

    // wait a second
    CLOCK.increment(1000);

    // updates the DB entry by setting it to ENABLED
    accessTierService.addUserToRegisteredTier(user);

    assertThat(userAccessTierDao.findAll()).hasSize(1);

    uat = userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat).isPresent();
    assertThat(uat.get().getUser()).isEqualTo(user);
    assertThat(uat.get().getAccessTier()).isEqualTo(registeredTier);
    assertThat(uat.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
    assertThat(uat.get().getFirstEnabled()).isEqualTo(firstEnabled);

    final Timestamp reEnabledTime = uat.get().getLastUpdated();
    assertThat(reEnabledTime).isGreaterThan(firstEnabled);
    assertThat(reEnabledTime).isGreaterThan(disabledTime);
  }

  @Test
  public void test_addUserToAllTiers_empty() {
    accessTierService.addUserToAllTiers(user);
    assertThat(userAccessTierDao.findAll()).isEmpty();
  }

  @Test
  public void test_addUserToAllTiers_two() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    final DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);
    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    accessTierService.addUserToAllTiers(user);

    assertThat(userAccessTierDao.findAll()).hasSize(2);

    Optional<DbUserAccessTier> uat_r =
        userAccessTierDao.getByUserAndAccessTier(user, registeredTier);
    assertThat(uat_r).isPresent();
    final DbUserAccessTier registeredMembership = uat_r.get();
    assertThat(registeredMembership.getUser()).isEqualTo(user);
    assertThat(registeredMembership.getAccessTier()).isEqualTo(registeredTier);
    assertThat(registeredMembership.getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);

    Optional<DbUserAccessTier> uat_c =
        userAccessTierDao.getByUserAndAccessTier(user, controlledTier);
    assertThat(uat_c).isPresent();
    final DbUserAccessTier controlledMembership = uat_c.get();
    assertThat(controlledMembership.getUser()).isEqualTo(user);
    assertThat(controlledMembership.getAccessTier()).isEqualTo(controlledTier);
    assertThat(controlledMembership.getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
  }

  private DbUserAccessTier addDaoEntry(DbUser user, DbAccessTier tier, TierAccessStatus status) {
    return userAccessTierDao.save(
        new DbUserAccessTier()
            .setUser(user)
            .setAccessTier(tier)
            .setTierAccessStatus(status)
            .setFirstEnabled(now())
            .setLastUpdated(now()));
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }
}
