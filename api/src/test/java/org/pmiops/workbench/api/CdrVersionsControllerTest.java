package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionMapper;
import org.pmiops.workbench.cdr.CdrVersionMapperImpl;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CdrVersionListResponse;
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
public class CdrVersionsControllerTest {

  @Autowired private AccessTierDao accessTierDao;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CdrVersionMapper cdrVersionMapper;

  @Autowired private CdrVersionsController cdrVersionsController;

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private DbCdrVersion defaultCdrVersion;
  private DbCdrVersion protectedCdrVersion;
  private static DbUser user;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    CdrVersionService.class,
    CdrVersionMapperImpl.class,
    CdrVersionsController.class
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
      return new WorkbenchConfig();
    }
  }

  @Before
  public void setUp() {
    user = new DbUser();
    user.setDataAccessLevelEnum(DataAccessLevel.REGISTERED);

    defaultCdrVersion =
        makeCdrVersion(
            1L, /* isDefault */
            true,
            "Test Registered CDR",
            123L,
            DataAccessLevel.REGISTERED,
            null,
            null,
            null);
    protectedCdrVersion =
        makeCdrVersion(
            2L, /* isDefault */
            false,
            "Test Protected CDR",
            456L,
            DataAccessLevel.PROTECTED,
            "microarray",
            null,
            null);
  }

  @Test
  public void testGetCdrVersionsRegistered() {
    assertResponse(cdrVersionsController.getCdrVersions().getBody(), defaultCdrVersion);
  }

  @Test
  public void testGetCdrVersions_microarray() {
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    List<CdrVersion> cdrVersions = cdrVersionsController.getCdrVersions().getBody().getItems();

    assertThat(
            cdrVersions.stream()
                .filter(v -> v.getName().equals("Test Registered CDR"))
                .findFirst()
                .get()
                .getHasMicroarrayData())
        .isFalse();

    assertThat(
            cdrVersions.stream()
                .filter(v -> v.getName().equals("Test Protected CDR"))
                .findFirst()
                .get()
                .getHasMicroarrayData())
        .isTrue();
  }

  @Test
  public void testGetCdrVersionsProtected() {
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    assertResponse(
        cdrVersionsController.getCdrVersions().getBody(), protectedCdrVersion, defaultCdrVersion);
  }

  @Test
  public void testGetCdrVersionsHasFitBit() {
    List<CdrVersion> cdrVersions = cdrVersionsController.getCdrVersions().getBody().getItems();
    // If not specified hasFitBitData is always false
    assertThat(cdrVersions.stream().filter(cdr -> !cdr.getHasFitbitData()).count())
        .isEqualTo(cdrVersions.size());
    makeCdrVersion(
        3L, true, "Test Registered FITBIT CDR", 123L, DataAccessLevel.REGISTERED, null, true, null);
    cdrVersions = cdrVersionsController.getCdrVersions().getBody().getItems();

    assertThat(
            cdrVersions.stream()
                .filter(cdr -> cdr.getName().equals("Test Registered FITBIT CDR"))
                .findFirst()
                .get()
                .getHasFitbitData())
        .isEqualTo((boolean) true);
  }

  @Test
  public void testGetCdrVersionshasCopeSurveyData() {
    List<CdrVersion> cdrVersions = cdrVersionsController.getCdrVersions().getBody().getItems();
    // If not specified hasCopeSurveyData is always false
    assertThat(cdrVersions.stream().filter(cdr -> !cdr.getHasCopeSurveyData()).count())
        .isEqualTo(cdrVersions.size());
    makeCdrVersion(
        3L, true, "Test Registered FITBIT CDR", 123L, DataAccessLevel.REGISTERED, null, true, true);
    cdrVersions = cdrVersionsController.getCdrVersions().getBody().getItems();

    assertThat(
            cdrVersions.stream()
                .filter(cdr -> cdr.getName().equals("Test Registered FITBIT CDR"))
                .findFirst()
                .get()
                .getHasCopeSurveyData())
        .isEqualTo((boolean) true);
  }

  @Test(expected = ForbiddenException.class)
  public void testGetCdrVersionsUnregistered() {
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    cdrVersionsController.getCdrVersions();
  }

  private void assertResponse(CdrVersionListResponse response, DbCdrVersion... versions) {
    assertThat(response.getItems())
        .containsExactly(Arrays.stream(versions).map(cdrVersionMapper::dbModelToClient).toArray())
        .inOrder();
    assertThat(response.getDefaultCdrVersionId())
        .isEqualTo(String.valueOf(defaultCdrVersion.getCdrVersionId()));
  }

  private DbCdrVersion makeCdrVersion(
      long cdrVersionId,
      boolean isDefault,
      String name,
      long creationTime,
      DataAccessLevel dataAccessLevel,
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
    cdrVersion.setAccessTier(TestMockFactory.createDefaultAccessTier(accessTierDao));
    cdrVersion.setName(name);
    cdrVersion.setNumParticipants(123);
    cdrVersion.setReleaseNumber((short) 1);
    cdrVersion.setMicroarrayBigqueryDataset(microarrayDataset);
    cdrVersion.setHasFitbitData(hasFitbit);
    cdrVersion.setHasCopeSurveyData(hasCopeSurveyData);
    cdrVersionDao.save(cdrVersion);
    return cdrVersion;
  }
}
