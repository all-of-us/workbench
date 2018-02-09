package org.pmiops.workbench.firecloud;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Enabled;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.test.Providers;

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
  @Mock
  private StatusApi statusApi;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    service = new FireCloudServiceImpl(Providers.of(workbenchConfig),
        Providers.of(profileApi), Providers.of(billingApi), Providers.of(groupsApi),
        Providers.of(statusApi), Providers.of(workspacesApi));
  }

  @Test(expected = ApiException.class)
  public void testIsRequesterEnabledInFirecloud_throws() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException());
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
}
