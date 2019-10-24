package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.cdrversions.CdrVersionRestController;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ImmutableCdrVersion;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
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
public class ImmutableCdrVersionRestControllerTest {

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CdrVersionRestController cdrVersionRestController;

  private CdrVersion defaultCdrVersionEntity;
  private CdrVersion protectedCdrVersionEntity;
  private User user;

  @TestConfiguration
  @Import({CdrVersionService.class, CdrVersionRestController.class})
  @MockBean({FireCloudService.class})
  static class Configuration {
    @Bean
    public User user() {
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
    user = new User();
    user.setDataAccessLevelEnum(DataAccessLevel.REGISTERED);
    cdrVersionRestController.setUserProvider(Providers.of(user));
    defaultCdrVersionEntity =
        makeCdrVersion(
            1L, /* isDefault */ true, "Test Registered CDR", 123L, DataAccessLevel.REGISTERED);
    protectedCdrVersionEntity =
        makeCdrVersion(
            2L, /* isDefault */ false, "Test Protected CDR", 456L, DataAccessLevel.PROTECTED);
  }

  @Test
  public void testGetCdrVersionsRegistered() {
    assertResponse(cdrVersionRestController.getCdrVersions().getBody(), defaultCdrVersionEntity);
  }

  @Test
  public void testGetCdrVersionsProtected() {
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    assertResponse(
        cdrVersionRestController.getCdrVersions().getBody(), protectedCdrVersionEntity,
        defaultCdrVersionEntity);
  }

  @Test(expected = ForbiddenException.class)
  public void testGetCdrVersionsUnregistered() {
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    cdrVersionRestController.getCdrVersions();
  }

  private void assertResponse(CdrVersionListResponse response, CdrVersion... versions) {
    assertThat(response.getItems())
        .containsExactly(
            Arrays.stream(versions)
                .map(ImmutableCdrVersion::fromEntity)
                .toArray())
        .inOrder();
    assertThat(response.getDefaultCdrVersionId())
        .isEqualTo(String.valueOf(defaultCdrVersionEntity.getCdrVersionId()));
  }

  private CdrVersion makeCdrVersion(
      long cdrVersionId,
      boolean isDefault,
      String name,
      long creationTime,
      DataAccessLevel dataAccessLevel) {
    CdrVersion cdrVersionEntity = new CdrVersion();
    cdrVersionEntity.setIsDefault(isDefault);
    cdrVersionEntity.setBigqueryDataset("a");
    cdrVersionEntity.setBigqueryProject("b");
    cdrVersionEntity.setCdrDbName("c");
    cdrVersionEntity.setCdrVersionId(cdrVersionId);
    cdrVersionEntity.setCreationTime(new Timestamp(creationTime));
    cdrVersionEntity.setDataAccessLevelEnum(dataAccessLevel);
    cdrVersionEntity.setName(name);
    cdrVersionEntity.setNumParticipants(123);
    cdrVersionEntity.setReleaseNumber((short) 1);
    cdrVersionDao.save(cdrVersionEntity);
    return cdrVersionEntity;
  }
}
