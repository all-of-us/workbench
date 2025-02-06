package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceDaoTest {

  @Autowired UserDao userDao;
  @Autowired ReportingTestFixture<DbUser, ReportingUser> userFixture;
  @Autowired WorkspaceDao workspaceDao;

  private DbUser dbUser;
  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, ReportingTestConfig.class})
  public static class conifg {}

  @BeforeEach
  public void setUp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    dbWorkspace = new DbWorkspace();
    dbWorkspace.setName("name");
    dbWorkspace.setWorkspaceNamespace("name");
    dbWorkspace.setFirecloudName("name");
    dbWorkspace.setCreationTime(timestamp);
    dbWorkspace.setLastModifiedTime(timestamp);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    dbUser = userDao.save(new DbUser());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_notCreator() {
    DbUser dbUser2 = userDao.save(userFixture.createEntity());
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser2);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_differentBillingAccount() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("personalAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_missingInitialCreditsRecord() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withUnexpiredCredits() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);
    long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    dbUser.setUserInitialCreditsExpiration(
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(new Timestamp(System.currentTimeMillis()))
            .setExpirationTime(new Timestamp(System.currentTimeMillis() + MILLIS_IN_A_DAY)));
    userDao.save(dbUser);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Set.of(dbUser));
  }

  @Test
  public void findCreatorsByActiveInitialCredits_withExpiredCredits() {
    long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    DbUserInitialCreditsExpiration dbUserInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(new Timestamp(System.currentTimeMillis()))
            .setExpirationTime(new Timestamp(System.currentTimeMillis() - MILLIS_IN_A_DAY))
            .setUser(dbUser);
    dbUser = dbUser.setUserInitialCreditsExpiration(dbUserInitialCreditsExpiration);
    dbUser = userDao.save(dbUser);
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    var y = workspaceDao.findCreatorsByActiveInitialCredits(
        List.of("initialCreditsAccount"), Set.of(dbUser));

    assertThat(y).isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_initialCreditsExhausted() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    dbWorkspace.setInitialCreditsExhausted(true);
    workspaceDao.save(dbWorkspace);

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("initialCreditsAccount"), Set.of(dbUser)))
        .isEqualTo(Collections.emptySet());
  }

  @Test
  public void findCreatorsByActiveInitialCredits_differentUser() {
    dbWorkspace.setBillingAccountName("initialCreditsAccount");
    dbWorkspace.setCreator(dbUser);
    workspaceDao.save(dbWorkspace);
    DbUser dbUser2 = userDao.save(userFixture.createEntity());

    assertThat(
            workspaceDao.findCreatorsByActiveInitialCredits(
                List.of("personalAccount"), Set.of(dbUser2)))
        .isEqualTo(Collections.emptySet());
  }
}
