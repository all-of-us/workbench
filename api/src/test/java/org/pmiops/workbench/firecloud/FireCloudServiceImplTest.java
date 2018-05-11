package org.pmiops.workbench.firecloud;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.rmi.ServerError;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.GatewayTimeoutException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Enabled;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

public class FireCloudServiceImplTest {


  private FireCloudServiceImpl service;

  @Mock
  private WorkbenchConfig workbenchConfig;
  @Mock
  private ProfileApi profileApi;
  @Mock
  private BillingApi billingApi;
  @Mock
  private WorkspacesApi workspacesApi;
  @Mock
  private GroupsApi groupsApi;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    service = new FireCloudServiceImpl(Providers.of(workbenchConfig),
        Providers.of(profileApi), Providers.of(billingApi), Providers.of(groupsApi),
        Providers.of(workspacesApi), new FirecloudRetryHandler(new NoBackOffPolicy()));
  }

  @Test(expected = ServerErrorException.class)
  public void testIsRequesterEnabledInFirecloud_throws() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException());
    service.isRequesterEnabledInFirecloud();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_throwsNotFound() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(404, "blah"));
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test(expected = ForbiddenException.class)
  public void testIsRequesterEnabledInFirecloud_throwsForbidden() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(403, "blah"));
    service.isRequesterEnabledInFirecloud();
  }

  @Test(expected = ConflictException.class)
  public void testIsRequesterEnabledInFirecloud_throwsConflict() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(409, "blah"));
    service.isRequesterEnabledInFirecloud();
  }

  public void testIsRequesterEnabledInFirecloud_throwsUnauthorized() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(401, "blah"));
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_retryTwiceSuccess() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenThrow(new ApiException(503, "blah"))
        .thenThrow(new ApiException(502, "foo"))
        .thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isTrue();
  }

  @Test(expected = GatewayTimeoutException.class)
  public void testIsRequesterEnabledInFirecloud_gatewayFail() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenThrow(new ApiException(504, "blah"));
    service.isRequesterEnabledInFirecloud();
  }

  @Test(expected = ServerUnavailableException.class)
  public void testIsRequesterEnabledInFirecloud_retryTwiceFail() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenThrow(new ApiException(503, "blah"))
        .thenThrow(new ApiException(502, "foo"))
        .thenThrow(new ApiException(503, "xxx"));
    service.isRequesterEnabledInFirecloud();
  }

  @Test(expected = GatewayTimeoutException.class)
  public void testIsRequesterEnabledInFirecloud_retryTwiceGatewayFail() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenThrow(new ApiException(503, "blah"))
        .thenThrow(new ApiException(502, "foo"))
        .thenThrow(new ApiException(504, "xxx"));
    service.isRequesterEnabledInFirecloud();
  }


  @Test
  public void testIsRequesterEnabledInFirecloud_enabledNull() throws ApiException {
    when(profileApi.me()).thenReturn(new Me());
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_enabledNoFlags() throws ApiException {
    Me me = new Me();
    me.setEnabled(new Enabled());
    when(profileApi.me()).thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_noLdap() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setAllUsersGroup(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_noGoogle() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setLdap(true);
    enabled.setAllUsersGroup(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isFalse();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_noAllUsers() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isTrue();
  }

  @Test
  public void testIsRequesterEnabledInFirecloud_all() throws ApiException {
    Me me = new Me();
    Enabled enabled = new Enabled();
    enabled.setGoogle(true);
    enabled.setLdap(true);
    enabled.setAllUsersGroup(true);
    me.setEnabled(enabled);
    when(profileApi.me()).thenReturn(me);
    assertThat(service.isRequesterEnabledInFirecloud()).isTrue();
  }

  @Test(expected = NotFoundException.class)
  public void testGetMe_throwsNotFound() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(404, "blah"));
    service.getMe();
  }

  @Test(expected = ForbiddenException.class)
  public void testGetMe_throwsForbidden() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(403, "blah"));
    service.getMe();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetMe_throwsUnauthorized() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(401, "blah"));
    service.getMe();
  }
}
