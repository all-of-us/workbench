package org.pmiops.workbench.ras;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.ACR_CLAIM;
import static org.pmiops.workbench.ras.RasLinkConstants.Id_TOKEN_FIELD_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_AUTH_CODE_SCOPES;
import static org.pmiops.workbench.ras.RasLinkConstants.TXN_CLAIM;
import static org.pmiops.workbench.ras.RasOidcClientConfig.RAS_OIDC_CLIENT;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.HttpTransport;
import jakarta.inject.Provider;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbIdentityVerification.DbIdentityVerificationSystem;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.identityverification.IdentityVerificationService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class RasLinkServiceTest {
  private static final Timestamp NOW = Timestamp.from(Instant.now());
  private static final FakeClock CLOCK = new FakeClock(NOW.toInstant(), ZoneId.systemDefault());

  private static final String TXN = "12345";
  private static final String AUTH_CODE = "code";
  private static final String REDIRECT_URL = "url";
  private static final String ACCESS_TOKEN =
      JWT.create().withClaim(TXN_CLAIM, TXN).sign(Algorithm.none());

  private static final String ID_ME_USERNAME = "foo@id.me";
  private static final String LOGIN_GOV_USERNAME = "foo@Login.Gov.com";
  private static final String ERA_COMMONS_USERNAME = "user2@eraCommons.com";

  private static final String USER_INFO_JSON_ID_ME =
      "{\"preferred_username\":\"" + ID_ME_USERNAME + "\",\"email\":\"foo@gmail.com\"}";
  private static final String USER_INFO_JSON_LOGIN_GOV =
      "{\"preferred_username\":\"" + LOGIN_GOV_USERNAME + "\",\"email\":\"foo@gmail.com\"}";
  private static final String USER_INFO_JSON_ERA =
      "{\"preferred_username\":\"" + ERA_COMMONS_USERNAME + "\",\"email\":\"foo2@gmail.com\"}";
  private static final String USER_INFO_JSON_LOGIN_GOV_WITH_ERA =
      "{\"preferred_username\":\""
          + LOGIN_GOV_USERNAME
          + "\",\"email\":\"foo@gmail.com\","
          + "\"federated_identities\":"
          + "{\"identities\":"
          + "{\"login.gov\":"
          + "{\"firstname\":\"\",\"lastname\":\"\",\"userid\":\"123\",\"mail\":\"foo@gmail.com\"},"
          + "\"era\":"
          + "{\"firstname\":\"\",\"lastname\":\"\",\"userid\":\"eraUserId\"}"
          + "}}}";
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

  private void mockCodeExchangeResponse(TokenResponse tokenResponse) throws IOException {
    when(mockOidcClient.codeExchange(AUTH_CODE, REDIRECT_URL, RAS_AUTH_CODE_SCOPES))
        .thenReturn(tokenResponse);
  }

  private void mockAccessTokenResponse(String jsonResponse) throws IOException {
    when(mockOidcClient.fetchUserInfo(ACCESS_TOKEN))
        .thenReturn(objectMapper.readTree(jsonResponse));
  }

  private long userId;
  private final Institution institution = new Institution();
  private static DbUser currentUser;
  private static List<DbAccessModule> accessModules;

  private RasLinkService rasLinkService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private AccessModuleService accessModuleService;
  @Mock private static OpenIdConnectClient mockOidcClient;

  @Mock private static IdentityVerificationService mockIdentityVerificationService;
  @Mock private static Provider<OpenIdConnectClient> mockOidcClientProvider;
  @Mock private static Provider<DbUser> mockUserProvider;
  @MockBean private InstitutionService mockInstitutionService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    AccessModuleServiceImpl.class,
    UserAccessModuleMapperImpl.class,
    CommonMappers.class,
    RasLinkService.class,
    UserServiceTestConfiguration.class,
    IdentityVerificationService.class
  })
  @MockBean({
    AccessTierService.class,
    DirectoryService.class,
    FireCloudService.class,
    InitialCreditsService.class,
    HttpTransport.class,
    MailService.class,
    UserServiceAuditor.class,
    InitialCreditsService.class,
    VwbUserService.class,
    TaskQueueService.class
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
      config.access.renewal.expiryDays = 365L;
      config.access.enableRasLoginGovLinking = true;
      config.billing.initialCreditsValidityPeriodDays = 57L;
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    rasLinkService =
        new RasLinkService(
            accessModuleService,
            userService,
            mockIdentityVerificationService,
            mockOidcClientProvider,
            mockUserProvider);
    when(mockOidcClientProvider.get()).thenReturn(mockOidcClient);
    when(mockInstitutionService.getByUser(any(DbUser.class))).thenReturn(Optional.of(institution));
    when(mockInstitutionService.validateInstitutionalEmail(
            eq(institution), anyString(), eq(REGISTERED_TIER_SHORT_NAME)))
        .thenReturn(true);

    currentUser = new DbUser();
    currentUser.setUsername("mock@mock.com");
    currentUser.setDisabled(false);
    currentUser = userDao.save(currentUser);

    when(mockUserProvider.get()).thenReturn(currentUser);
    userId = currentUser.getUserId();

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
  }

  @Test
  public void testLinkRasIdMeSuccess() throws Exception {
    mockCodeExchangeResponse(TOKEN_RESPONSE_IAL2);
    mockAccessTokenResponse(USER_INFO_JSON_ID_ME);

    rasLinkService.linkRasAccount(AUTH_CODE, REDIRECT_URL);

    DbUser expectedUser = userDao.findUserByUserId(userId);
    assertThat(expectedUser.getRasLinkUsername()).isEqualTo(ID_ME_USERNAME);
    assertModuleCompletionTime(DbAccessModuleName.IDENTITY, NOW);
    assertModuleCompletionTime(DbAccessModuleName.ERA_COMMONS, null);
    verify(mockIdentityVerificationService)
        .updateIdentityVerificationSystem(expectedUser, DbIdentityVerificationSystem.ID_ME);
    verify(mockIdentityVerificationService, never())
        .updateIdentityVerificationSystem(expectedUser, DbIdentityVerificationSystem.LOGIN_GOV);
  }

  @Test
  public void testLinkRasLoginGovSuccess() throws Exception {
    mockCodeExchangeResponse(TOKEN_RESPONSE_IAL2);
    mockAccessTokenResponse(USER_INFO_JSON_LOGIN_GOV);
    rasLinkService.linkRasAccount(AUTH_CODE, REDIRECT_URL);

    DbUser expectedUser = userDao.findUserByUserId(userId);
    assertThat(expectedUser.getRasLinkUsername()).isEqualTo(LOGIN_GOV_USERNAME);
    assertModuleCompletionTime(DbAccessModuleName.IDENTITY, NOW);
    assertModuleCompletionTime(DbAccessModuleName.RAS_LOGIN_GOV, NOW);
    assertModuleCompletionTime(DbAccessModuleName.ERA_COMMONS, null);
    verify(mockIdentityVerificationService)
        .updateIdentityVerificationSystem(expectedUser, DbIdentityVerificationSystem.LOGIN_GOV);
    verify(mockIdentityVerificationService, never())
        .updateIdentityVerificationSystem(expectedUser, DbIdentityVerificationSystem.ID_ME);
  }

  @Test
  public void testLinkRasFail_ial1() throws Exception {
    mockCodeExchangeResponse(TOKEN_RESPONSE_IAL1);
    verify(mockIdentityVerificationService, never()).updateIdentityVerificationSystem(any(), any());
    assertThrows(
        ForbiddenException.class, () -> rasLinkService.linkRasAccount(AUTH_CODE, REDIRECT_URL));
  }

  @Test
  public void testLinkRasFail_notLoginGovOrIdMe() throws Exception {
    mockCodeExchangeResponse(TOKEN_RESPONSE_IAL2);
    mockAccessTokenResponse(USER_INFO_JSON_ERA);
    verify(mockIdentityVerificationService, never()).updateIdentityVerificationSystem(any(), any());
    assertThrows(
        ForbiddenException.class, () -> rasLinkService.linkRasAccount(AUTH_CODE, REDIRECT_URL));
  }

  private void assertModuleCompletionTime(DbAccessModuleName moduleName, Timestamp timestamp) {
    Optional<DbUserAccessModule> dbAccessModule =
        userAccessModuleDao.getByUserAndAccessModule(
            currentUser, accessModuleDao.findOneByName(moduleName).get());
    if (dbAccessModule.isEmpty()) {
      assertThat(timestamp).isNull();
    } else {
      // Timestamps from the database do not include nanoseconds, so this has to be truncated to
      // milliseconds for a valid comparison.
      Timestamp normalized = new Timestamp(timestamp.getTime());
      assertThat(dbAccessModule.get().getCompletionTime()).isEqualTo(normalized);
    }
  }
}
