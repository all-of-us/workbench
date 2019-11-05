package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CdrVersionListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CdrVersionsControllerTest {

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CdrVersionsController cdrVersionsController;

  private CdrVersion defaultCdrVersion;
  private CdrVersion protectedCdrVersion;
  private DbUser user;

  @TestConfiguration
  @Import({CdrVersionService.class, CdrVersionsController.class})
  @MockBean({FireCloudService.class})
  static class Configuration {
    @Bean
    public DbUser user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
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
    cdrVersionsController.setUserProvider(Providers.of(user));
    defaultCdrVersion =
        makeCdrVersion(
            1L, /* isDefault */ true, "Test Registered CDR", 123L, DataAccessLevel.REGISTERED);
    protectedCdrVersion =
        makeCdrVersion(
            2L, /* isDefault */ false, "Test Protected CDR", 456L, DataAccessLevel.PROTECTED);
  }

  @Test
  public void testGetCdrVersionsRegistered() {
    assertResponse(cdrVersionsController.getCdrVersions().getBody(), defaultCdrVersion);
  }

  @Test
  public void testGetCdrVersionsProtected() {
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    assertResponse(
        cdrVersionsController.getCdrVersions().getBody(), protectedCdrVersion, defaultCdrVersion);
  }

  @Test(expected = ForbiddenException.class)
  public void testGetCdrVersionsUnregistered() {
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    cdrVersionsController.getCdrVersions();
  }

  private void assertResponse(CdrVersionListResponse response, CdrVersion... versions) {
    assertThat(response.getItems())
        .containsExactly(
            Arrays.stream(versions).map(CdrVersionsController.TO_CLIENT_CDR_VERSION).toArray())
        .inOrder();
    assertThat(response.getDefaultCdrVersionId())
        .isEqualTo(String.valueOf(defaultCdrVersion.getCdrVersionId()));
  }

  private CdrVersion makeCdrVersion(
      long cdrVersionId,
      boolean isDefault,
      String name,
      long creationTime,
      DataAccessLevel dataAccessLevel) {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setIsDefault(isDefault);
    cdrVersion.setBigqueryDataset("a");
    cdrVersion.setBigqueryProject("b");
    cdrVersion.setCdrDbName("c");
    cdrVersion.setCdrVersionId(cdrVersionId);
    cdrVersion.setCreationTime(new Timestamp(creationTime));
    cdrVersion.setDataAccessLevelEnum(dataAccessLevel);
    cdrVersion.setName(name);
    cdrVersion.setNumParticipants(123);
    cdrVersion.setReleaseNumber((short) 1);
    cdrVersionDao.save(cdrVersion);
    return cdrVersion;
  }
}
