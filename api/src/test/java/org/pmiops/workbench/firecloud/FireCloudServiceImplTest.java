package org.pmiops.workbench.firecloud;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpTransport;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.BillingV2Api;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.model.FirecloudCreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudSystemStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.web.context.WebApplicationContext;

public class FireCloudServiceImplTest extends SpringTest {

  private static final String EMAIL_ADDRESS = "abc@fake-research-aou.org";

  @Autowired
  @Qualifier(FireCloudCacheConfig.SERVICE_ACCOUNT_REQUEST_SCOPED_GROUP_CACHE)
  private LoadingCache<String, FirecloudManagedGroupWithMembers> firecloudGroupCache;

  @Autowired private FireCloudService service;

  private static WorkbenchConfig workbenchConfig;

  @MockBean private BillingApi billingApi;

  @MockBean
  @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_BILLING_v2_API)
  private BillingV2Api billingV2Api;

  @MockBean private HttpTransport httpTransport;
  @MockBean private IamCredentialsClient iamCredentialsClient;
  @MockBean private NihApi nihApi;
  @MockBean private ProfileApi profileApi;
  @MockBean private StatusApi statusApi;

  @MockBean
  @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_GROUPS_API)
  private GroupsApi groupsApi;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @TestConfiguration
  @Import({FireCloudCacheConfig.class, FireCloudServiceImpl.class, RetryConfig.class})
  static class Configuration {
    @Bean
    public CustomScopeConfigurer customScopeConfigurer() {
      // Configure the request scope so we can reuse the cache config; this is not available by
      // default in Spring unit tests.
      CustomScopeConfigurer scopeConfigurer = new CustomScopeConfigurer();
      scopeConfigurer.setScopes(
          ImmutableMap.of(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope()));
      return scopeConfigurer;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.firecloud.baseUrl = "https://api.firecloud.org";
    workbenchConfig.firecloud.debugEndpoints = true;
    workbenchConfig.firecloud.timeoutInSeconds = 20;
    workbenchConfig.billing.accountId = "test-billing-account";
  }

  @AfterEach
  public void tearDown() {
    firecloudGroupCache.invalidateAll();
  }

  @Test
  public void testStatus_success() throws ApiException {
    when(statusApi.status()).thenReturn(new FirecloudSystemStatus());
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  @Test
  public void testStatus_handleApiException() throws ApiException {
    when(statusApi.status()).thenThrow(new ApiException(500, null, "{\"ok\": false}"));
    assertThat(service.getFirecloudStatus()).isFalse();
  }

  @Test
  public void testStatus_handleJsonException() throws ApiException {
    when(statusApi.status()).thenThrow(new ApiException(500, null, "unparseable response"));
    assertThat(service.getFirecloudStatus()).isFalse();
  }

  @Test
  public void testGetMe_throwsNotFound() throws ApiException {
    assertThrows(
        NotFoundException.class,
        () -> {
          when(profileApi.me()).thenThrow(new ApiException(404, "blah"));
          service.getMe();
        });
  }

  @Test
  public void testGetMe_throwsForbidden() throws ApiException {
    assertThrows(
        ForbiddenException.class,
        () -> {
          when(profileApi.me()).thenThrow(new ApiException(403, "blah"));
          service.getMe();
        });
  }

  @Test
  public void testGetMe_throwsUnauthorized() throws ApiException {
    assertThrows(
        UnauthorizedException.class,
        () -> {
          when(profileApi.me()).thenThrow(new ApiException(401, "blah"));
          service.getMe();
        });
  }

  @Test
  public void testIsUserMemberOfGroup_none() throws Exception {
    when(groupsApi.getGroup("group")).thenReturn(new FirecloudManagedGroupWithMembers());
    assertThat(service.isUserMemberOfGroupWithCache(EMAIL_ADDRESS, "group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noNameMatch() throws Exception {
    FirecloudManagedGroupWithMembers group = new FirecloudManagedGroupWithMembers();
    group.setMembersEmails(Arrays.asList("asdf@fake-research-aou.org"));
    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroupWithCache(EMAIL_ADDRESS, "group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_matchInAdminList() throws Exception {
    FirecloudManagedGroupWithMembers group = new FirecloudManagedGroupWithMembers();
    group.setAdminsEmails(Arrays.asList(EMAIL_ADDRESS));

    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroupWithCache(EMAIL_ADDRESS, "group")).isTrue();
  }

  @Test
  public void testIsUserMemberOfGroup_matchInMemberList() throws Exception {
    FirecloudManagedGroupWithMembers group = new FirecloudManagedGroupWithMembers();
    group.setMembersEmails(Arrays.asList(EMAIL_ADDRESS));

    when(groupsApi.getGroup("group")).thenReturn(group);
    assertThat(service.isUserMemberOfGroupWithCache(EMAIL_ADDRESS, "group")).isTrue();
  }

  @Test
  public void testNihStatus() throws Exception {
    FirecloudNihStatus status =
        new FirecloudNihStatus().linkedNihUsername("test").linkExpireTime(500L);
    when(nihApi.nihStatus()).thenReturn(status);
    assertThat(service.getNihStatus()).isNotNull();
    assertThat(service.getNihStatus()).isEqualTo(status);
  }

  @Test
  public void testNihStatusNotFound() throws Exception {
    when(nihApi.nihStatus()).thenThrow(new ApiException(404, "Not Found"));
    assertThat(service.getNihStatus()).isNull();
  }

  @Test
  public void testNihStatusException() throws Exception {
    assertThrows(
        ServerErrorException.class,
        () -> {
          when(nihApi.nihStatus()).thenThrow(new ApiException(500, "Internal Server Error"));
          service.getNihStatus();
        });
  }

  @Test
  public void testCreateAllOfUsBillingProject() throws Exception {
    final String servicePerimeter = "a-cloud-with-a-fence-around-it";
    service.createAllOfUsBillingProject("project-name", servicePerimeter);

    ArgumentCaptor<FirecloudCreateRawlsBillingProjectFullRequest> captor =
        ArgumentCaptor.forClass(FirecloudCreateRawlsBillingProjectFullRequest.class);
    verify(billingApi).createBillingProjectFull(captor.capture());
    FirecloudCreateRawlsBillingProjectFullRequest request = captor.getValue();

    // N.B. FireCloudServiceImpl doesn't add the project prefix; this is done by callers such
    // as BillingProjectBufferService.
    assertThat(request.getProjectName()).isEqualTo("project-name");
    // FireCloudServiceImpl always adds the "billingAccounts/" prefix to the billing account
    // from config.
    assertThat(request.getBillingAccount()).isEqualTo("billingAccounts/test-billing-account");
    assertThat(request.isEnableFlowLogs()).isTrue();
    assertThat(request.isHighSecurityNetwork()).isTrue();
    assertThat(request.getServicePerimeter()).isEqualTo(servicePerimeter);
  }

  @Test
  public void testCreateAllOfUsBillingProject_v2BillingApi() throws Exception {
    final String servicePerimeter = "a-cloud-with-a-fence-around-it";
    // confirm that this value is no longer how we choose perimeters
    workbenchConfig.featureFlags.enableFireCloudV2Billing = true;

    service.createAllOfUsBillingProject("project-name", servicePerimeter);

    ArgumentCaptor<FirecloudCreateRawlsBillingProjectFullRequest> captor =
        ArgumentCaptor.forClass(FirecloudCreateRawlsBillingProjectFullRequest.class);
    verify(billingV2Api).createBillingProjectFullV2(captor.capture());
    FirecloudCreateRawlsBillingProjectFullRequest request = captor.getValue();

    // N.B. FireCloudServiceImpl doesn't add the project prefix; this is done by callers such
    // as BillingProjectBufferService.
    assertThat(request.getProjectName()).isEqualTo("project-name");
    // FireCloudServiceImpl always adds the "billingAccounts/" prefix to the billing account
    // from config.
    assertThat(request.getBillingAccount()).isEqualTo("billingAccounts/test-billing-account");
    assertThat(request.isEnableFlowLogs()).isTrue();
    assertThat(request.isHighSecurityNetwork()).isTrue();
    assertThat(request.getServicePerimeter()).isEqualTo(servicePerimeter);
  }
}
