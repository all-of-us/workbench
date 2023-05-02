package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CdrVersionTier;
import org.pmiops.workbench.model.CdrVersionTiersResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CdrVersionServiceTest {

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessTierService accessTierService;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionMapper cdrVersionMapper;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private FireCloudService fireCloudService;
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
  @MockBean({
    FireCloudService.class,
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
            false);

    controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

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
            false);
  }

  @Test
  public void testSetCdrVersionDefault() {
    addMembershipForTest(registeredTier);
    cdrVersionService.setCdrVersion(defaultCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(defaultCdrVersion);
  }

  @Test
  public void testSetCdrVersionDefaultForbiddenUserNotInTier() {
    assertThrows(
        ForbiddenException.class, () -> cdrVersionService.setCdrVersion(defaultCdrVersion));
  }

  // these tests fail because the user is in the right tier according to the AoU DB
  // but the user is not in the right auth domain according to Terra

  @Test
  public void testSetCdrVersionDefaultForbiddenNotInGroup() {
    assertThrows(
        ForbiddenException.class,
        () -> {
          accessTierService.addUserToTier(user, registeredTier);
          when(fireCloudService.isUserMemberOfGroupWithCache(
                  user.getUsername(), registeredTier.getAuthDomainName()))
              .thenReturn(false);
          cdrVersionService.setCdrVersion(defaultCdrVersion);
        });
  }

  @Test
  public void testSetCdrVersionControlled() {
    addMembershipForTest(controlledTier);
    cdrVersionService.setCdrVersion(controlledCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(controlledCdrVersion);
  }

  @Test
  public void testSetCdrVersionControlledForbiddenUserNotInTier() {
    assertThrows(
        ForbiddenException.class, () -> cdrVersionService.setCdrVersion(controlledCdrVersion));
  }

  // these tests fail because the user is in the right tier according to the AoU DB
  // but the user is not in the right auth domain according to Terra

  @Test
  public void testSetCdrVersionControlledForbiddenNotInGroup() {
    assertThrows(
        ForbiddenException.class,
        () -> {
          accessTierService.addUserToTier(user, controlledTier);
          when(fireCloudService.isUserMemberOfGroupWithCache(
                  user.getUsername(), controlledTier.getAuthDomainName()))
              .thenReturn(false);
          cdrVersionService.setCdrVersion(controlledCdrVersion);
        });
  }

  @Test
  public void testGetCdrVersionsByTierAllTiers() {
    addMembershipForTest(registeredTier);
    addMembershipForTest(controlledTier);
    CdrVersionTiersResponse response = cdrVersionService.getCdrVersionsByTier();
    assertExpectedResponse(response);
  }

  // we still expect to see all tiers returned for an RT-only user

  @Test
  public void testGetCdrVersionsByTierRegisteredOnly() {
    addMembershipForTest(registeredTier);
    CdrVersionTiersResponse response = cdrVersionService.getCdrVersionsByTier();
    assertExpectedResponse(response);
  }

  @Test
  public void testGetCdrVersionsByTierUnregistered() {
    assertThrows(ForbiddenException.class, cdrVersionService::getCdrVersionsByTier);
  }

  @Test
  public void testGetCdrVersionsHasFitBit() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasFitbitData);
  }

  @Test
  public void testGetCdrVersionsHasFitbitSleepData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasFitbitSleepData);
  }

  @Test
  public void testGetCdrVersionsHasSurveyConductData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasSurveyConductData);
  }

  @Test
  public void testGetCdrVersionsHasCopeSurveyData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasCopeSurveyData);
  }

  @Test
  public void testGetCdrVersionsHasWgsData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasWgsData);
  }

  private void assertExpectedResponse(CdrVersionTiersResponse response) {
    List<String> shortNames =
        response.getTiers().stream()
            .map(CdrVersionTier::getAccessTierShortName)
            .collect(Collectors.toList());
    assertThat(shortNames)
        .containsExactly(registeredTier.getShortName(), controlledTier.getShortName());

    assertExpectedTier(
        response, registeredTier.getShortName(), defaultCdrVersion, nonDefaultCdrVersion);
    assertExpectedTier(
        response,
        controlledTier.getShortName(),
        controlledCdrVersion,
        controlledNonDefaultCdrVersion);
  }

  private void assertExpectedTier(
      CdrVersionTiersResponse response,
      String shortName,
      DbCdrVersion defaultVersion,
      DbCdrVersion otherVersion) {
    CdrVersionTier tier = parseTier(response, shortName);
    assertThat(tier.getVersions())
        .containsExactly(
            cdrVersionMapper.dbModelToClient(defaultVersion),
            cdrVersionMapper.dbModelToClient(otherVersion));
    CdrVersion expectedDefault = cdrVersionMapper.dbModelToClient(defaultVersion);
    assertThat(tier.getDefaultCdrVersionId()).isEqualTo(expectedDefault.getCdrVersionId());
    assertThat(tier.getDefaultCdrVersionCreationTime())
        .isEqualTo(expectedDefault.getCreationTime());
  }

  private void testGetCdrVersionsHasDataType(Predicate<CdrVersion> hasType) {
    addMembershipForTest(registeredTier);
    final List<CdrVersion> cdrVersions =
        parseTierVersions(cdrVersionService.getCdrVersionsByTier(), registeredTier.getShortName());
    // hasFitBitData, hasCopeSurveyData, hasMicroarrayData, and hasWgsData are false by default
    assertThat(cdrVersions.stream().anyMatch(hasType)).isFalse();

    makeCdrVersion(
        5L, true, "Test CDR With Data Types", registeredTier, "wgs", true, true, true, true);
    final List<CdrVersion> newVersions =
        parseTierVersions(cdrVersionService.getCdrVersionsByTier(), registeredTier.getShortName());

    Optional<CdrVersion> cdrVersionMaybe =
        newVersions.stream()
            .filter(cdr -> cdr.getName().equals("Test CDR With Data Types"))
            .findFirst();
    assertThat(cdrVersionMaybe).isPresent();
    assertThat(hasType.test(cdrVersionMaybe.get())).isTrue();
  }

  private CdrVersionTier parseTier(
      CdrVersionTiersResponse cdrVersionsByTier, String accessTierShortName) {
    Optional<CdrVersionTier> tierVersions =
        cdrVersionsByTier.getTiers().stream()
            .filter(x -> x.getAccessTierShortName().equals(accessTierShortName))
            .findFirst();
    assertThat(tierVersions).isPresent();
    return tierVersions.get();
  }

  private List<CdrVersion> parseTierVersions(
      CdrVersionTiersResponse cdrVersionsByTier, String accessTierShortName) {
    return parseTier(cdrVersionsByTier, accessTierShortName).getVersions();
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
      boolean hasSurevyConductData) {
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
    return cdrVersionDao.save(cdrVersion);
  }

  private void addMembershipForTest(DbAccessTier tier) {
    accessTierService.addUserToTier(user, tier);

    when(fireCloudService.isUserMemberOfGroupWithCache(
            user.getUsername(), tier.getAuthDomainName()))
        .thenReturn(true);
  }
}
