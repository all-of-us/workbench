package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({CommonConfig.class})
@DataJpaTest
public class UserAccessModuleDaoTest extends SpringTest {
  @Autowired private UserDao userDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;

  private static final Timestamp CURRENT_TIMESTAMP = Timestamp.from(Instant.now());
  private DbUser user;
  private DbAccessModule twoFactorAuthModule;
  private DbAccessModule rtTrainingModule;

  @BeforeEach
  public void setup() {
    user = new DbUser();
    user.setUserId(100);
    user = userDao.save(user);

    TestMockFactory.createAccessModules(accessModuleDao);
    twoFactorAuthModule = accessModuleDao.findOneByName(AccessModuleName.TWO_FACTOR_AUTH).get();
    rtTrainingModule = accessModuleDao.findOneByName(AccessModuleName.RT_COMPLIANCE_TRAINING).get();
  }

  @Test
  public void testInsertAndGet() {
    DbUserAccessModule twoFactorAuthUserAccess =
        new DbUserAccessModule()
            .setAccessModule(twoFactorAuthModule)
            .setCompletionTime(CURRENT_TIMESTAMP)
            .setUser(user);
    DbUserAccessModule trainingUserAccess =
        new DbUserAccessModule()
            .setAccessModule(rtTrainingModule)
            .setCompletionTime(CURRENT_TIMESTAMP)
            .setBypassTime(CURRENT_TIMESTAMP)
            .setUser(user);
    List<DbUserAccessModule> userAccess =
        ImmutableList.of(twoFactorAuthUserAccess, trainingUserAccess);
    userAccessModuleDao.saveAll(userAccess);
    assertThat(userAccessModuleDao.getAllByUser(user)).containsExactlyElementsIn(userAccess);
    assertThat(userAccessModuleDao.getByUserAndAccessModule(user, twoFactorAuthModule).get())
        .isEqualTo(twoFactorAuthUserAccess);
  }
}
