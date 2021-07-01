package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import({CommonConfig.class})
@DataJpaTest
public class UserAccessModuleDaoTest extends SpringTest {
  @Autowired private UserDao userDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;

  private DbUser user;
  private DbAccessModule registeredModule;
  private DbAccessModule controlledModule;

  @BeforeEach
  public void setup() {
    user = new DbUser();
    user.setUserId(100);
    user = userDao.save(user);

    registeredModule = TestMockFactory.createRegisteredModuleForTests(accessModuleDao);
    controlledModule = TestMockFactory.createControlledModuleForTests(accessModuleDao);
  }

  @Test
  public void testGetAllByUser() {
    assertThat(userAccessModuleDao.getAllByUser(user, registeredModule)).isEmpty();
  }

  @Test
  public void test_getByUserAndAccessModule_RT() {
    // arbitrary nonzero time amount
    final Instant recently = Instant.now().minusSeconds(10);

    userAccessModuleDao.save(
        new DbUserAccessModule()
            .setUser(user)
            .setAccessModule(registeredModule)
            .setModuleAccessStatus(ModuleAccessStatus.ENABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    Optional<DbUserAccessModule> entryMaybe =
        userAccessModuleDao.getByUserAndAccessModule(user, registeredModule);
    assertThat(entryMaybe).isPresent();
    DbUserAccessModule entry = entryMaybe.get();
    assertThat(entry.getUser()).isEqualTo(user);
    assertThat(entry.getAccessModule()).isEqualTo(registeredModule);
    assertThat(entry.getModuleAccessStatusEnum()).isEqualTo(ModuleAccessStatus.ENABLED);
    assertThat(entry.getFirstEnabled().toInstant().isAfter(recently)).isTrue();
    assertThat(entry.getLastUpdated().toInstant().isAfter(recently)).isTrue();
  }

  @Test
  public void test_getByUserAndAccessModule_RT_disabled() {
    userAccessModuleDao.save(
        new DbUserAccessModule()
            .setUser(user)
            .setAccessModule(registeredModule)
            .setModuleAccessStatus(ModuleAccessStatus.DISABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    Optional<DbUserAccessModule> entryMaybe =
        userAccessModuleDao.getByUserAndAccessModule(user, registeredModule);
    assertThat(entryMaybe).isPresent();
    DbUserAccessModule entry = entryMaybe.get();
    assertThat(entry.getModuleAccessStatusEnum()).isEqualTo(ModuleAccessStatus.DISABLED);
  }

  @Test
  public void test_getByUserAndAccessModule_CT_no_access() {
    userAccessModuleDao.save(
        new DbUserAccessModule()
            .setUser(user)
            .setAccessModule(registeredModule)
            .setModuleAccessStatus(ModuleAccessStatus.ENABLED)
            .setFirstEnabled(now())
            .setLastUpdated(now()));

    assertThat(userAccessModuleDao.getByUserAndAccessModule(user, controlledModule)).isEmpty();
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }
}
