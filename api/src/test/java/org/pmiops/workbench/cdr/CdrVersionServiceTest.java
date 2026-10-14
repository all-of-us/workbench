package org.pmiops.workbench.cdr;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.VwbAccessService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CdrVersionServiceTest {
  @MockitoBean private VwbAccessService vwbAccessService;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessTierService accessTierService;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionMapper cdrVersionMapper;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private UserDao userDao;

  private static DbUser user;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static final WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();

  private DbAccessTier registeredTier;
  private DbAccessTier controlledTier;

  private DbCdrVersion defaultCdrVersion;
  private DbCdrVersion nonDefaultCdrVersion;
  private DbCdrVersion controlledCdrVersion;
  private DbCdrVersion controlledNonDefaultCdrVersion;

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    CommonMappers.class,
    CdrVersionService.class,
    CdrVersionMapperImpl.class,
    FakeClockConfiguration.class
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DbUser user() {
      return user;
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @BeforeEach
  public void setUp() {

    user = new DbUser();
    user.setUsername("user");
    user = userDao.save(user);

    registeredTier = accessTierDao.save(createRegisteredTier());

    defaultCdrVersion =
        makeCdrVersion(
            1L, /* isDefault */
            true,
            "Test Registered CDR",
            registeredTier,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false);
    nonDefaultCdrVersion =
        makeCdrVersion(
            2L, /* isDefault */
            false,
            "Old Registered CDR",
            registeredTier,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false);

    controlledTier = accessTierDao.save(createControlledTier());

    controlledCdrVersion =
        makeCdrVersion(
            3L, /* isDefault */
            true,
            "Test Controlled CDR",
            controlledTier,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false);
    controlledNonDefaultCdrVersion =
        makeCdrVersion(
            4L, /* isDefault */
            false,
            "Old Controlled CDR",
            controlledTier,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false);
  }

  // we still expect to see all tiers returned for an RT-only user

  @Test
  public void testGetCdrVersionsByTierUnregistered() {
    assertThrows(ForbiddenException.class, cdrVersionService::getCdrVersionsByTier);
  }

  private DbCdrVersion makeCdrVersion(
      long cdrVersionId,
      boolean isDefault,
      String name,
      DbAccessTier accessTier,
      String wgsDataset,
      boolean hasFitbit,
      boolean hasCopeSurveyData,
      boolean hasFitbitSleepData,
      boolean hasSurevyConductData,
      boolean tanagraEnabled,
      boolean hasFitbitDeviceData,
      boolean hasMHWBAndETMData) {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setIsDefault(isDefault);
    cdrVersion.setBigqueryDataset("a");
    cdrVersion.setBigqueryProject("b");
    cdrVersion.setCdrDbName("c");
    cdrVersion.setCdrVersionId(cdrVersionId);
    cdrVersion.setAccessTier(accessTier);
    cdrVersion.setName(name);
    cdrVersion.setWgsBigqueryDataset(wgsDataset);
    cdrVersion.setHasFitbitData(hasFitbit);
    cdrVersion.setHasCopeSurveyData(hasCopeSurveyData);
    cdrVersion.setHasFitbitSleepData(hasFitbitSleepData);
    cdrVersion.setHasSurveyConductData(hasSurevyConductData);
    cdrVersion.setTanagraEnabled(tanagraEnabled);
    cdrVersion.setHasFitbitDeviceData(hasFitbitDeviceData);
    cdrVersion.setHasMHWBAndETMData(hasMHWBAndETMData);
    return cdrVersionDao.save(cdrVersion);
  }
}
