package org.pmiops.workbench.ras;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM;
import static org.pmiops.workbench.ras.RasLinkConstants.Id_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_AUTH_CODE_SCOPES;
import static org.pmiops.workbench.ras.RasOidcClientConfig.RAS_OIDC_CLIENT;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.HttpTransport;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class RasLinkServiceTest {
  private static final Timestamp NOW = Timestamp.from(Instant.now());
  private static final FakeClock CLOCK = new FakeClock(NOW.toInstant(), ZoneId.systemDefault());

  private static final String AUTH_CODE = "code";
  private static final String REDIRECT_URL = "url";
  private static final String ACCESS_TOKEN = "access_token_1";
  private static final String LOGIN_GOV_USERNAME = "foo@Login.Gov.com";
  private static final String ERA_COMMONS_USERNAME = "user2@eraCommons.com";
  private static final String USER_INFO_JSON_LOGIN_GOV =
      "{\"preferred_username\":\"" + LOGIN_GOV_USERNAME + "\",\"email\":\"foo@gmail.com\"}";
  private static final String USER_INFO_JSON_ERA =
      "{\"preferred_username\":\"" + ERA_COMMONS_USERNAME + "\",\"email\":\"foo2@gmail.com\"}";
  private static final String ID_TOKEN_JWT_IAL_1 =
      JWT.create()
          .withClaim(
              ACR_CLAIM,
              "https://stsstg.nih.gov/assurance/ial/1 https://stsstg.nih.gov/assurance/aal/1")
          .sign(Algorithm.none());
  private static final String ID_TOKEN_JWT_IAL_2 =
      JWT.create()
          .withClaim(
              ACR_CLAIM,
              "https://stsstg.nih.gov/assurance/ial/2 https://stsstg.nih.gov/assurance/aal/1")
          .sign(Algorithm.none());
  private static final TokenResponse TOKEN_RESPONSE_IAL1 =
      new TokenResponse().setAccessToken(ACCESS_TOKEN).set(Id_TOKEN_FIELD_NAME, ID_TOKEN_JWT_IAL_1);
  private static final TokenResponse TOKEN_RESPONSE_IAL2 =
      new TokenResponse().setAccessToken(ACCESS_TOKEN).set(Id_TOKEN_FIELD_NAME, ID_TOKEN_JWT_IAL_2);

  private long userId;
  private static DbUser currentUser;

  private RasLinkService rasLinkService;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Mock private static OpenIdConnectClient mockOidcClient;
  @Mock private static Provider<OpenIdConnectClient> mockOidcClientProvider;
  @Mock private static HttpTransport mockHttpTransport;
  @Mock private OpenIdConnectClient mockRasOidcClient;

  @TestConfiguration
  @Import({
    RasLinkService.class,
    UserServiceTestConfiguration.class,
  })
  @MockBean({
    AccessTierService.class,
    ComplianceService.class,
    DirectoryService.class,
    FireCloudService.class,
    FreeTierBillingService.class,
    HttpTransport.class,
    MailService.class,
    UserServiceAuditor.class,
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Qualifier(RAS_OIDC_CLIENT)
    OpenIdConnectClient rasOidcClient() {
      return mockOidcClient;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig getWorkbenchConfig() {
      WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
      config.accessRenewal.expiryDays = (long) 365;
      return config;
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }
  }

  @Before
  public void setUp() throws Exception {
    rasLinkService = new RasLinkService(userService, mockOidcClientProvider);
    when(mockOidcClientProvider.get()).thenReturn(mockOidcClient);

    currentUser = new DbUser();
    currentUser.setUsername("mock@mock.com");
    currentUser.setDisabled(false);
    currentUser = userDao.save(currentUser);
    userId = currentUser.getUserId();
  }

  @Test
  public void testLinkRasSuccess() throws Exception {
    when(mockOidcClient.codeExchange(AUTH_CODE, REDIRECT_URL, RAS_AUTH_CODE_SCOPES))
        .thenReturn(TOKEN_RESPONSE_IAL2);
    when(mockOidcClient.fetchUserInfo(ACCESS_TOKEN))
        .thenReturn(objectMapper.readTree(USER_INFO_JSON_LOGIN_GOV));
    rasLinkService.linkRasLoginGovAccount(AUTH_CODE, REDIRECT_URL);

    assertThat(userDao.findUserByUserId(userId).getRasLinkLoginGovUsername())
        .isEqualTo(LOGIN_GOV_USERNAME);
    assertThat(userDao.findUserByUserId(userId).getRasLinkLoginGovCompletionTime()).isEqualTo(NOW);
  }

  @Test
  public void testLinkRasFail_ial1() throws Exception {
    when(mockOidcClient.codeExchange(AUTH_CODE, REDIRECT_URL, RAS_AUTH_CODE_SCOPES))
        .thenReturn(TOKEN_RESPONSE_IAL1);
    assertThrows(
        ForbiddenException.class,
        () -> rasLinkService.linkRasLoginGovAccount(AUTH_CODE, REDIRECT_URL));
  }

  @Test
  public void testLinkRasFail_notLoginGov() throws Exception {
    when(mockOidcClient.codeExchange(AUTH_CODE, REDIRECT_URL, RAS_AUTH_CODE_SCOPES))
        .thenReturn(TOKEN_RESPONSE_IAL2);
    when(mockOidcClient.fetchUserInfo(ACCESS_TOKEN))
        .thenReturn(objectMapper.readTree(USER_INFO_JSON_ERA));
    assertThrows(
        ForbiddenException.class,
        () -> rasLinkService.linkRasLoginGovAccount(AUTH_CODE, REDIRECT_URL));
  }
}
