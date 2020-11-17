package org.pmiops.workbench.trackedproperties;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.rmi.AccessException;
import java.time.Clock;
import java.util.Collections;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.profile.AddressMapperImpl;
import org.pmiops.workbench.profile.DemographicSurveyMapperImpl;
import org.pmiops.workbench.profile.PageVisitMapperImpl;
import org.pmiops.workbench.profile.ProfileMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class UserPropertyServiceTest {

  private static DbUser agentUser;
  private static DbUser targetUser;
  @MockBean private UserDao mockUserDao;
  @MockBean private ProfileAuditor mockProfileAuditor;

  @Autowired private UserGivenNamePropertyService userGivenNamePropertyService;
  @Autowired private UserPropertyService userPropertyService;
  @Autowired private Provider<DbUser> agentUserProvider;

  @TestConfiguration
  @Import({
      AddressMapperImpl.class,
      CommonMappers.class,
      DemographicSurveyMapperImpl.class,
      PageVisitMapperImpl.class,
      ProfileMapperImpl.class,
      PropertyProcessorServiceImpl.class,
      UserGivenNamePropertyService.class,
      UserPropertyServiceImpl.class
  })
  @MockBean({
      Clock.class
  })
  public static class config {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return agentUser;
    }
  }

  @Before
  public void setup()  {
    agentUser = new DbUser();
    agentUser.setUserId(101L);
    agentUser.setUsername("admin1@@aou.biz");
    agentUser.setAuthorities(Collections.singleton(DbStorageEnums.authorityToStorage(Authority.ACCESS_CONTROL_ADMIN)));

    targetUser = new DbUser();
    targetUser.setUserId(202L);
    targetUser.setUsername("someone@@aou.biz");
    targetUser.setGivenName("Beatrix");
  }

  @Test
  public void testSetGivenName() throws IllegalAccessException, AccessException {
    final DbUser updated = userPropertyService.setGivenName(targetUser, "Fred");
    assertThat(updated.getGivenName()).isEqualTo("Fred");
    verify(mockProfileAuditor).fireUpdateAction(any(), any(Profile.class));
    verify(mockProfileAuditor).fireUpdateAction(any(), any(Profile.class));
    verify(mockUserDao).save(any(DbUser.class));
  }
}
