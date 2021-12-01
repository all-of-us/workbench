package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
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
  private DbCdrVersion controlledCdrVersion;

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
    @Scope("prototype")
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

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    defaultCdrVersion =
        makeCdrVersion(
            1L,
            /* isDefault */ true,
            "Test Registered CDR",
            123L,
            registeredTier,
            null,
            null,
            null,
            "",
            "");

    controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);

    controlledCdrVersion =
        makeCdrVersion(
            2L,
            /* isDefault */ true,
            "Test Controlled CDR",
            456L,
            controlledTier,
            null,
            null,
            null,
            "gs://lol",
            "gs://lol");
  }

  @Test
  public void testSetCdrVersionDefault() {
    addMembershipForTest(registeredTier);
    cdrVersionService.setCdrVersion(defaultCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(defaultCdrVersion);
  }

  @Test
  public void testSetCdrVersionDefaultId() {
    addMembershipForTest(registeredTier);
    cdrVersionService.setCdrVersion(defaultCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(defaultCdrVersion);
  }

  @Test
  public void testSetCdrVersionDefaultForbiddenNotInTier() {
    assertThrows(
        ForbiddenException.class,
        () -> cdrVersionService.setCdrVersion(defaultCdrVersion));
  }

  @Test
  public void testSetCdrVersionDefaultIdForbiddenNotInTier() {
    assertThrows(
        ForbiddenException.class,
        () -> cdrVersionService.setCdrVersion(defaultCdrVersion));
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
  public void testSetCdrVersionDefaultIdForbiddenNotInGroup() {
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
  public void testSetCdrVersionControlledId() {
    addMembershipForTest(controlledTier);
    cdrVersionService.setCdrVersion(controlledCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(controlledCdrVersion);
  }

  @Test
  public void testSetCdrVersionControlledForbiddenNotInTier() {
    assertThrows(
        ForbiddenException.class,
        () -> cdrVersionService.setCdrVersion(controlledCdrVersion));
  }

  @Test
  public void testSetCdrVersionControlledIdForbiddenNotInTier() {
    assertThrows(
        ForbiddenException.class,
        () -> cdrVersionService.setCdrVersion(controlledCdrVersion));
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
  public void testSetCdrVersionControlledIdForbiddenNotInGroup() {
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
  public void testGetCdrVersionsHasCopeSurveyData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasCopeSurveyData);
  }

  @Test
  public void testGetCdrVersionsHasWgsData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasWgsData);
  }

  private void assertExpectedResponse(CdrVersionTiersResponse response) {
    List<String> responseTiers =
        response.getTiers().stream()
            .map(CdrVersionTier::getAccessTierShortName)
            .collect(Collectors.toList());
    assertThat(responseTiers)
        .containsExactly(registeredTier.getShortName(), controlledTier.getShortName());

    List<CdrVersion> responseVersions =
        response.getTiers().stream()
            .map(CdrVersionTier::getVersions)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    assertThat(responseVersions)
        .containsExactly(
            cdrVersionMapper.dbModelToClient(defaultCdrVersion),
            cdrVersionMapper.dbModelToClient(controlledCdrVersion));
  }

  private void testGetCdrVersionsHasDataType(Predicate<CdrVersion> hasType) {
    addMembershipForTest(registeredTier);
    final List<CdrVersion> cdrVersions =
        parseRegisteredTier(cdrVersionService.getCdrVersionsByTier());
    // hasFitBitData, hasCopeSurveyData, hasMicroarrayData, and hasWgsData are false by default
    assertThat(cdrVersions.stream().anyMatch(hasType)).isFalse();

    makeCdrVersion(
        3L, true, "Test CDR With Data Types", 123L, registeredTier, "wgs", true, true, "", "");
    final List<CdrVersion> newVersions =
        parseRegisteredTier(cdrVersionService.getCdrVersionsByTier());

    Optional<CdrVersion> cdrVersionMaybe =
        newVersions.stream()
            .filter(cdr -> cdr.getName().equals("Test CDR With Data Types"))
            .findFirst();
    assertThat(cdrVersionMaybe).isPresent();
    assertThat(hasType.test(cdrVersionMaybe.get())).isTrue();
  }

  private List<CdrVersion> parseRegisteredTier(CdrVersionTiersResponse cdrVersionsByTier) {
    Optional<CdrVersionTier> tierVersions =
        cdrVersionsByTier.getTiers().stream()
            .filter(x -> x.getAccessTierShortName().equals(registeredTier.getShortName()))
            .findFirst();
    assertThat(tierVersions).isPresent();
    return tierVersions.get().getVersions();
  }

  private DbCdrVersion makeCdrVersion(
      long cdrVersionId,
      boolean isDefault,
      String name,
      long creationTime,
      DbAccessTier accessTier,
      String wgsDataset,
      Boolean hasFitbit,
      Boolean hasCopeSurveyData,
      String allSamplesWgsDataBucket,
      String singleSampleArrayDataBucket) {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setIsDefault(isDefault);
    cdrVersion.setBigqueryDataset("a");
    cdrVersion.setBigqueryProject("b");
    cdrVersion.setCdrDbName("c");
    cdrVersion.setCdrVersionId(cdrVersionId);
    cdrVersion.setCreationTime(new Timestamp(creationTime));
    cdrVersion.setAccessTier(accessTier);
    cdrVersion.setName(name);
    cdrVersion.setNumParticipants(123);
    cdrVersion.setReleaseNumber((short) 1);
    cdrVersion.setWgsBigqueryDataset(wgsDataset);
    cdrVersion.setHasFitbitData(hasFitbit);
    cdrVersion.setHasCopeSurveyData(hasCopeSurveyData);
    cdrVersion.setAllSamplesWgsDataBucket(allSamplesWgsDataBucket);
    cdrVersion.setSingleSampleArrayDataBucket(singleSampleArrayDataBucket);
    return cdrVersionDao.save(cdrVersion);
  }

  private void addMembershipForTest(DbAccessTier tier) {
    accessTierService.addUserToTier(user, tier);

    when(fireCloudService.isUserMemberOfGroupWithCache(
            user.getUsername(), tier.getAuthDomainName()))
        .thenReturn(true);
  }
}
