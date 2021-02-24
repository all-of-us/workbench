package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CdrVersionListResponse;
import org.pmiops.workbench.model.CdrVersionMapResponse;
import org.pmiops.workbench.model.CdrVersionMapResponseInner;
import org.pmiops.workbench.model.DataAccessLevel;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CdrVersionServiceTest {

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionMapper cdrVersionMapper;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private FireCloudService fireCloudService;

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
  })
  @MockBean({FireCloudService.class})
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

  @Before
  public void setUp() {
    user = new DbUser();
    user.setUsername("user");
    user.setDataAccessLevelEnum(DataAccessLevel.REGISTERED);

    registeredTier = TestMockFactory.createDefaultAccessTier(accessTierDao);
    defaultCdrVersion =
        makeCdrVersion(
            1L,
            /* isDefault */ true,
            "Test Registered CDR",
            123L,
            registeredTier,
            null,
            null,
            null);

    controlledTier =
        accessTierDao.save(
            new DbAccessTier()
                .setAccessTierId(2)
                .setShortName("controlled")
                .setDisplayName("Controlled Tier")
                .setAuthDomainName("Controlled Tier Auth Domain")
                .setAuthDomainGroupEmail("ct-users@fake-research-aou.org")
                .setServicePerimeter("controlled/tier/perimeter"));

    controlledCdrVersion =
        makeCdrVersion(
            2L,
            /* isDefault */ true,
            "Test Controlled CDR",
            456L,
            controlledTier,
            null,
            null,
            null);
  }

  @Test
  public void testSetCdrVersionDefault() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), registeredTier.getAuthDomainName()))
        .thenReturn(true);
    cdrVersionService.setCdrVersion(defaultCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(defaultCdrVersion);
  }

  @Test
  public void testSetCdrVersionDefaultId() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), registeredTier.getAuthDomainName()))
        .thenReturn(true);
    cdrVersionService.setCdrVersion(defaultCdrVersion.getCdrVersionId());
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(defaultCdrVersion);
  }

  @Test
  public void testSetCdrVersionControlled() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(true);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;
    cdrVersionService.setCdrVersion(controlledCdrVersion);
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(controlledCdrVersion);
  }

  @Test
  public void testSetCdrVersionControlledId() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(true);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;
    cdrVersionService.setCdrVersion(controlledCdrVersion.getCdrVersionId());
    assertThat(CdrVersionContext.getCdrVersion()).isEqualTo(controlledCdrVersion);
  }

  @Test(expected = ForbiddenException.class)
  public void testSetCdrVersionControlledForbiddenNotInTier() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(true);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = false;
    cdrVersionService.setCdrVersion(controlledCdrVersion);
  }

  @Test(expected = ForbiddenException.class)
  public void testSetCdrVersionControlledIdForbiddenNotInTier() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(true);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = false;
    cdrVersionService.setCdrVersion(controlledCdrVersion.getCdrVersionId());
  }

  @Test(expected = ForbiddenException.class)
  public void testSetCdrVersionControlledForbiddenNotInGroup() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(false);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;
    cdrVersionService.setCdrVersion(controlledCdrVersion);
  }

  @Test(expected = ForbiddenException.class)
  public void testSetCdrVersionControlledIdForbiddenNotInGroup() {
    when(fireCloudService.isUserMemberOfGroup(
            user.getUsername(), controlledTier.getAuthDomainName()))
        .thenReturn(false);
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;
    cdrVersionService.setCdrVersion(controlledCdrVersion.getCdrVersionId());
  }

  // Tests for deprecated registered-tier-only getCdrVersions()

  @Test
  public void testGetCdrVersionsRegisteredOnly() {
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = false;

    CdrVersionListResponse response = cdrVersionService.getCdrVersions();
    assertDefaultCdrOnly(response);
  }

  @Test
  public void testGetCdrVersionsRegisteredAllTiers() {
    // this flag does not affect the deprecated version, so the result is the same
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;

    CdrVersionListResponse response = cdrVersionService.getCdrVersions();
    assertDefaultCdrOnly(response);
  }

  private void assertDefaultCdrOnly(CdrVersionListResponse response) {
    List<CdrVersion> expected =
        ImmutableList.of(cdrVersionMapper.dbModelToClient(defaultCdrVersion));
    assertThat(response.getItems()).containsExactlyElementsIn(expected);

    String expectedId = String.valueOf(defaultCdrVersion.getCdrVersionId());
    assertThat(response.getDefaultCdrVersionId()).isEqualTo(expectedId);
  }

  @Test(expected = ForbiddenException.class)
  public void testGetCdrVersionsUnregistered() {
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    cdrVersionService.getCdrVersions();
  }

  // Tests for multi-tier getCdrVersionsByTier()

  @Test
  public void testGetCdrVersionsByTierRegisteredOnly() {
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = false;
    CdrVersionMapResponse response = cdrVersionService.getCdrVersionsByTier();
    assertResponseMultiTier(response, ImmutableList.of("registered"), defaultCdrVersion);
  }

  @Test
  public void testGetCdrVersionsByTierAllTiers() {
    config.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;
    CdrVersionMapResponse response = cdrVersionService.getCdrVersionsByTier();
    assertResponseMultiTier(
        response,
        ImmutableList.of("registered", "controlled"),
        defaultCdrVersion,
        controlledCdrVersion);
  }

  @Test(expected = ForbiddenException.class)
  public void testGetCdrVersionsByTierUnregistered() {
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    cdrVersionService.getCdrVersionsByTier();
  }

  private void assertResponseMultiTier(
      CdrVersionMapResponse response, List<String> accessTierShortNames, DbCdrVersion... versions) {
    List<String> responseTiers =
        response.stream()
            .map(CdrVersionMapResponseInner::getAccessTierShortName)
            .collect(Collectors.toList());
    assertThat(responseTiers).containsExactlyElementsIn(accessTierShortNames);

    List<CdrVersion> responseVersions =
        response.stream()
            .map(CdrVersionMapResponseInner::getVersions)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    List<CdrVersion> expectedVersions =
        Arrays.stream(versions).map(cdrVersionMapper::dbModelToClient).collect(Collectors.toList());
    assertThat(responseVersions).containsExactlyElementsIn(expectedVersions);
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
  public void testGetCdrVersionsHasMicroarrayData() {
    testGetCdrVersionsHasDataType(CdrVersion::getHasMicroarrayData);
  }

  private void testGetCdrVersionsHasDataType(Predicate<CdrVersion> hasType) {
    final List<CdrVersion> cdrVersions =
        parseRegisteredTier(cdrVersionService.getCdrVersionsByTier());
    // hasFitBitData, hasCopeSurveyData, and hasMicroarrayData are false by default
    assertThat(cdrVersions.stream().anyMatch(hasType)).isFalse();

    makeCdrVersion(
        3L, true, "Test CDR With Data Types", 123L, registeredTier, "microarray", true, true);
    final List<CdrVersion> newVersions =
        parseRegisteredTier(cdrVersionService.getCdrVersionsByTier());

    Optional<CdrVersion> cdrVersionMaybe =
        newVersions.stream()
            .filter(cdr -> cdr.getName().equals("Test CDR With Data Types"))
            .findFirst();
    assertThat(cdrVersionMaybe).isPresent();
    assertThat(hasType.test(cdrVersionMaybe.get())).isTrue();
  }

  private List<CdrVersion> parseRegisteredTier(CdrVersionMapResponse cdrVersionsByTier) {
    Optional<CdrVersionMapResponseInner> tierVersions =
        cdrVersionsByTier.stream()
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
      String microarrayDataset,
      Boolean hasFitbit,
      Boolean hasCopeSurveyData) {
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
    cdrVersion.setMicroarrayBigqueryDataset(microarrayDataset);
    cdrVersion.setHasFitbitData(hasFitbit);
    cdrVersion.setHasCopeSurveyData(hasCopeSurveyData);
    return cdrVersionDao.save(cdrVersion);
  }
}
